package no.nav.mulighetsrommet.api.arrangorflate

import arrow.core.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import kotliquery.TransactionalSession
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrFlateRefusjonKravAft
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrFlateRefusjonKravAft.Beregning
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrFlateRefusjonKravKompakt
import no.nav.mulighetsrommet.api.arrangorflate.model.RefusjonKravDeltakelse
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.refusjon.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.refusjon.HentPersonBolkResponse
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslag
import no.nav.mulighetsrommet.api.refusjon.model.DeltakerDto
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningFri
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravDto
import no.nav.mulighetsrommet.api.refusjon.task.JournalforRefusjonskrav
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

fun Route.arrangorflateRoutes() {
    val tilsagnService: TilsagnService by inject()
    val arrangorService: ArrangorService by inject()
    val pdl: HentAdressebeskyttetPersonBolkPdlQuery by inject()
    val journalforRefusjonskrav: JournalforRefusjonskrav by inject()
    val db: ApiDatabase by inject()
    val pdfClient: PdfGenClient by inject()

    suspend fun RoutingContext.arrangorerMedTilgang(): List<ArrangorDto> = db.session {
        call.principal<ArrangorflatePrincipal>()
            ?.organisasjonsnummer
            ?.map {
                arrangorService.getArrangorOrSyncFromBrreg(it).getOrElse {
                    throw StatusException(HttpStatusCode.InternalServerError, "Feil ved henting av arrangor_id")
                }
            }
            ?: throw StatusException(HttpStatusCode.Unauthorized, "Mangler altinn tilgang")
    }

    fun RoutingContext.requireTilgangHosArrangor(organisasjonsnummer: Organisasjonsnummer) {
        call.principal<ArrangorflatePrincipal>()
            ?.organisasjonsnummer
            ?.find { it == organisasjonsnummer }
            ?: throw StatusException(HttpStatusCode.Forbidden, "Ikke tilgang til bedrift")
    }

    route("/arrangorflate") {
        get("/tilgang-arrangor") {
            call.respond(arrangorerMedTilgang())
        }

        route("/arrangor/{orgnr}") {
            get("/refusjonskrav") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                val krav = db.session {
                    queries.refusjonskrav.getByArrangorIds(orgnr).map {
                        ArrFlateRefusjonKravKompakt.fromRefusjonskravDto(it)
                    }
                }

                call.respond(krav)
            }

            get("/tilsagn") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                val tilsagn = db.session {
                    queries.tilsagn.getAllArrangorflateTilsagn(orgnr)
                }

                call.respond(tilsagn)
            }
        }

        route("/refusjonskrav/{id}") {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = db.session {
                    queries.refusjonskrav.get(id) ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                }

                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val oppsummering = toRefusjonskrav(db, pdl, krav)

                call.respond(oppsummering)
            }

            get("/relevante-forslag") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = db.session {
                    queries.refusjonskrav.get(id) ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                }

                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val forslagByDeltakerId = db.session {
                    queries.deltakerForslag.getForslagByGjennomforing(krav.gjennomforing.id)
                }

                val relevanteForslag = forslagByDeltakerId
                    .map { (deltakerId, forslag) ->
                        RelevanteForslag(
                            deltakerId = deltakerId,
                            antallRelevanteForslag = forslag.count { it.relevantForDeltakelse(krav) },
                        )
                    }

                call.respond(relevanteForslag)
            }

            post("/godkjenn-refusjon") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = db.session {
                    queries.refusjonskrav.get(id) ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                }

                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val request = call.receive<GodkjennRefusjonskrav>()

                val forslagByDeltakerId = db.session {
                    queries.deltakerForslag.getForslagByGjennomforing(krav.gjennomforing.id)
                }

                validerGodkjennRefusjonskrav(
                    request,
                    krav,
                    forslagByDeltakerId,
                ).onLeft {
                    return@post call.respondWithStatusResponse(ValidationError(errors = it).left())
                }

                db.transaction {
                    queries.refusjonskrav.setGodkjentAvArrangor(id, LocalDateTime.now())
                    queries.refusjonskrav.setBetalingsInformasjon(
                        id,
                        request.betalingsinformasjon.kontonummer,
                        request.betalingsinformasjon.kid,
                    )
                    journalforRefusjonskrav.schedule(krav.id, Instant.now(), session as TransactionalSession)
                }

                call.respond(HttpStatusCode.OK)
            }

            get("/kvittering") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = db.session {
                    queries.refusjonskrav.get(id) ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                }

                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val tilsagn = tilsagnService.getArrangorflateTilsagnTilRefusjon(
                    gjennomforingId = krav.gjennomforing.id,
                    periode = krav.periode,
                )
                val refusjonsKravAft = toRefusjonskrav(db, pdl, krav)
                val pdfContent = pdfClient.getRefusjonKvittering(refusjonsKravAft, tilsagn)

                call.response.headers.append(
                    "Content-Disposition",
                    "attachment; filename=\"kvittering.pdf\"",
                )

                call.respondBytes(pdfContent, contentType = ContentType.Application.Pdf)
            }

            get("/tilsagn") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = db.session {
                    queries.refusjonskrav.get(id) ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                }

                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val tilsagn = tilsagnService.getArrangorflateTilsagnTilRefusjon(
                    gjennomforingId = krav.gjennomforing.id,
                    periode = krav.periode,
                )

                call.respond(tilsagn)
            }
        }

        route("/tilsagn/{id}") {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val tilsagn = db.session {
                    queries.tilsagn.getArrangorflateTilsagn(id) ?: throw NotFoundException("Fant ikke tilsagn")
                }

                requireTilgangHosArrangor(tilsagn.arrangor.organisasjonsnummer)

                call.respond(tilsagn)
            }
        }
    }
}

fun DeltakerForslag.relevantForDeltakelse(
    refusjonskrav: RefusjonskravDto,
): Boolean = when (refusjonskrav.beregning) {
    is RefusjonKravBeregningAft -> this.relevantForDeltakelse(refusjonskrav.beregning)
    is RefusjonKravBeregningFri -> false
}

fun DeltakerForslag.relevantForDeltakelse(
    beregning: RefusjonKravBeregningAft,
): Boolean {
    val deltakelser = beregning.input.deltakelser
        .find { it.deltakelseId == this.deltakerId }
        ?: return false

    val periode = beregning.input.periode
    val sisteSluttDato = deltakelser.perioder.maxOf { it.slutt }
    val forsteStartDato = deltakelser.perioder.minOf { it.start }

    return when (this.endring) {
        is Melding.Forslag.Endring.AvsluttDeltakelse -> {
            val sluttDato = this.endring.sluttdato

            this.endring.harDeltatt == false || (sluttDato != null && sluttDato.isBefore(sisteSluttDato))
        }

        is Melding.Forslag.Endring.Deltakelsesmengde -> {
            this.endring.gyldigFra?.isBefore(sisteSluttDato) ?: true
        }

        is Melding.Forslag.Endring.ForlengDeltakelse -> {
            this.endring.sluttdato.isAfter(sisteSluttDato) && this.endring.sluttdato.isBefore(periode.slutt)
        }

        is Melding.Forslag.Endring.IkkeAktuell -> {
            true
        }

        is Melding.Forslag.Endring.Sluttarsak -> {
            false
        }

        is Melding.Forslag.Endring.Sluttdato -> {
            this.endring.sluttdato.isBefore(sisteSluttDato)
        }

        is Melding.Forslag.Endring.Startdato -> {
            this.endring.startdato.isAfter(forsteStartDato)
        }

        Melding.Forslag.Endring.FjernOppstartsdato -> true
    }
}

fun validerGodkjennRefusjonskrav(
    request: GodkjennRefusjonskrav,
    krav: RefusjonskravDto,
    forslagByDeltakerId: Map<UUID, List<DeltakerForslag>>,
): Either<List<FieldError>, GodkjennRefusjonskrav> {
    val finnesRelevanteForslag = forslagByDeltakerId
        .any { (_, forslag) ->
            forslag.count { it.relevantForDeltakelse(krav) } > 0
        }

    return if (finnesRelevanteForslag) {
        listOf(
            FieldError.ofPointer(
                "/info",
                "Det finnes forslag på deltakere som påvirker refusjonen. Disse må behandles av Nav før refusjonskravet kan sendes inn.",
            ),
        ).left()
    } else if (request.digest != krav.beregning.getDigest()) {
        listOf(
            FieldError.ofPointer(
                "/info",
                "Informasjonen i kravet har endret seg. Vennligst se over på nytt.",
            ),
        ).left()
    } else {
        request.right()
    }
}

suspend fun toRefusjonskrav(
    db: ApiDatabase,
    pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    krav: RefusjonskravDto,
): ArrFlateRefusjonKravAft = when (val beregning = krav.beregning) {
    is RefusjonKravBeregningAft -> {
        val deltakere = db.session { queries.deltaker.getAll(gjennomforingId = krav.gjennomforing.id) }

        val deltakereById = deltakere.associateBy { it.id }
        val personerByNorskIdent: Map<NorskIdent, RefusjonKravDeltakelse.Person> = getPersoner(pdl, deltakere)
        val perioderById = beregning.input.deltakelser.associateBy { it.deltakelseId }
        val manedsverkById = beregning.output.deltakelser.associateBy { it.deltakelseId }

        val deltakelser = perioderById.map { (id, deltakelse) ->
            val deltaker = deltakereById.getValue(id)
            val manedsverk = manedsverkById.getValue(id).manedsverk
            val person = personerByNorskIdent[deltaker.norskIdent]

            val forstePeriode = deltakelse.perioder.first()
            val sistePeriode = deltakelse.perioder.last()

            RefusjonKravDeltakelse(
                id = id,
                startDato = deltaker.startDato,
                sluttDato = deltaker.startDato,
                forstePeriodeStartDato = forstePeriode.start,
                sistePeriodeSluttDato = sistePeriode.slutt.minusDays(1),
                sistePeriodeDeltakelsesprosent = sistePeriode.deltakelsesprosent,
                manedsverk = manedsverk,
                person = person,
                // TODO data om veileder hos arrangør
                veileder = null,
            )
        }

        val antallManedsverk = deltakelser
            .map { BigDecimal(it.manedsverk) }
            .sumOf { it }
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

        ArrFlateRefusjonKravAft(
            id = krav.id,
            status = krav.status,
            fristForGodkjenning = krav.fristForGodkjenning,
            tiltakstype = krav.tiltakstype,
            gjennomforing = krav.gjennomforing,
            arrangor = krav.arrangor,
            deltakelser = deltakelser,
            beregning = Beregning(
                periodeStart = beregning.input.periode.start,
                periodeSlutt = beregning.input.periode.getLastDate(),
                antallManedsverk = antallManedsverk,
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
            ),
            betalingsinformasjon = krav.betalingsinformasjon,
        )
    }

    is RefusjonKravBeregningFri -> throw StatusException(
        status = HttpStatusCode.NotImplemented,
        detail = "Visning av frimodell er ikke implementert",
    )
}

private suspend fun getPersoner(
    pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    deltakere: List<DeltakerDto>,
): Map<NorskIdent, RefusjonKravDeltakelse.Person> {
    val identer = deltakere
        .mapNotNull { deltaker -> deltaker.norskIdent?.value?.let { PdlIdent(it) } }
        .toNonEmptySetOrNull()
        ?: return mapOf()

    return pdl.hentPersonBolk(identer)
        .map {
            buildMap {
                it.entries.forEach { (ident, person) ->
                    val refusjonskravPerson = toRefusjonskravPerson(person)
                    put(NorskIdent(ident.value), refusjonskravPerson)
                }
            }
        }
        .getOrElse {
            throw StatusException(
                status = HttpStatusCode.InternalServerError,
                detail = "Klarte ikke hente informasjon om deltakere i refusjonskravet.",
            )
        }
}

private fun toRefusjonskravPerson(person: HentPersonBolkResponse.Person): RefusjonKravDeltakelse.Person {
    val gradering = person.adressebeskyttelse.firstOrNull()?.gradering ?: PdlGradering.UGRADERT
    return when (gradering) {
        PdlGradering.UGRADERT -> {
            val navn = person.navn.first().let { navn ->
                val fornavnOgMellomnavn = listOfNotNull(navn.fornavn, navn.mellomnavn).joinToString(" ")
                listOf(navn.etternavn, fornavnOgMellomnavn).joinToString(", ")
            }
            val foedselsdato = person.foedselsdato.first()
            RefusjonKravDeltakelse.Person(
                navn = navn,
                fodselsaar = foedselsdato.foedselsaar,
                fodselsdato = foedselsdato.foedselsdato,
            )
        }

        else -> RefusjonKravDeltakelse.Person(
            navn = "Adressebeskyttet",
            fodselsaar = null,
            fodselsdato = null,
        )
    }
}

@Serializable
data class GodkjennRefusjonskrav(
    val betalingsinformasjon: Betalingsinformasjon,
    val digest: String,
) {
    @Serializable
    data class Betalingsinformasjon(
        val kontonummer: Kontonummer,
        val kid: Kid?,
    )
}

@Serializable
data class RelevanteForslag(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    val antallRelevanteForslag: Int,
)
