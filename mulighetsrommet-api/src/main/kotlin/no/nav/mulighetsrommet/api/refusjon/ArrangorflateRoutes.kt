package no.nav.mulighetsrommet.api.refusjon

import arrow.core.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.clients.dokark.*
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.pdfgen.Pdfgen
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslag
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslagRepository
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.refusjon.model.*
import no.nav.mulighetsrommet.api.refusjon.task.JournalforRefusjonskrav
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.Kid
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.amt.Melding
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.exception.StatusException
import org.koin.ktor.ext.inject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

fun Route.arrangorflateRoutes() {
    val tilsagnService: TilsagnService by inject()
    val arrangorService: ArrangorService by inject()
    val refusjonskrav: RefusjonskravRepository by inject()
    val deltakerRepository: DeltakerRepository by inject()
    val deltakerForslagRepository: DeltakerForslagRepository by inject()
    val pdl: HentAdressebeskyttetPersonBolkPdlQuery by inject()
    val journalforRefusjonskrav: JournalforRefusjonskrav by inject()
    val db: Database by inject()

    suspend fun RoutingContext.arrangorerMedTilgang(): List<ArrangorDto> {
        return call.principal<ArrangorflatePrincipal>()
            ?.organisasjonsnummer
            ?.map {
                arrangorService.getOrSyncArrangorFromBrreg(it)
                    .getOrElse {
                        throw StatusException(HttpStatusCode.InternalServerError, "Feil ved henting av arrangor_id")
                    }
            }
            ?: throw StatusException(HttpStatusCode.Unauthorized)
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

                val krav = refusjonskrav.getByArrangorIds(orgnr)
                    .map { toRefusjonskravKompakt(it) }

                call.respond(krav)
            }

            get("/tilsagn") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }
                requireTilgangHosArrangor(orgnr)
                call.respond(tilsagnService.getAllArrangorflateTilsagn(orgnr))
            }
        }

        route("/refusjonskrav/{id}") {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val oppsummering = toRefusjonskrav(pdl, deltakerRepository, krav)

                call.respond(oppsummering)
            }

            get("/relevante-forslag") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val deltakere = deltakerRepository.getAll(krav.gjennomforing.id)
                val forslagByDeltakerId = deltakerForslagRepository.get(deltakere.map { it.id })

                val relevanteForslag = deltakere
                    .map { deltaker ->
                        val forslag = forslagByDeltakerId[deltaker.id] ?: emptyList()
                        RelevanteForslag(
                            deltakerId = deltaker.id,
                            antallRelevanteForslag = forslag.count { it.relevantForDeltakelse(krav) },
                        )
                    }

                call.respond(relevanteForslag)
            }

            post("/godkjenn-refusjon") {
                val id = call.parameters.getOrFail<UUID>("id")
                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val request = call.receive<GodkjennRefusjonskrav>()
                validerGodkjennRefusjonskrav(request, krav)
                    .onLeft { return@post call.respondWithStatusResponse(BadRequest(errors = it).left()) }

                db.transactionSuspend { tx ->
                    refusjonskrav.setGodkjentAvArrangor(id, LocalDateTime.now(), tx)
                    refusjonskrav.setBetalingsInformasjon(
                        id,
                        request.betalingsinformasjon.kontonummer,
                        request.betalingsinformasjon.kid,
                        tx,
                    )

                    journalforRefusjonskrav.schedule(krav.id, Instant.now(), tx)
                }

                call.respond(HttpStatusCode.OK)
            }

            get("/kvittering") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val tilsagn = tilsagnService.getArrangorflateTilsagnTilRefusjon(
                    gjennomforingId = krav.gjennomforing.id,
                    periode = krav.beregning.input.periode,
                )

                val refusjonsKravAft = toRefusjonskrav(pdl, deltakerRepository, krav)
                val pdf = Pdfgen.refusjonKvittering(refusjonsKravAft, tilsagn)

                call.response.headers.append(
                    "Content-Disposition",
                    "attachment; filename=\"kvittering.pdf\"",
                )
                call.respondBytes(pdf, contentType = ContentType.Application.Pdf)
            }

            get("/tilsagn") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val tilsagn = tilsagnService.getArrangorflateTilsagnTilRefusjon(
                    gjennomforingId = krav.gjennomforing.id,
                    periode = krav.beregning.input.periode,
                )
                call.respond(tilsagn)
            }
        }

        route("/tilsagn/{id}") {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val tilsagn = tilsagnService.getArrangorflateTilsagn(id)
                    ?: throw NotFoundException("Fant ikke tilsagn")
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
    }
}

fun validerGodkjennRefusjonskrav(
    request: GodkjennRefusjonskrav,
    krav: RefusjonskravDto,
): Either<List<ValidationError>, GodkjennRefusjonskrav> {
    return if (request.digest != krav.beregning.getDigest()) {
        listOf(
            ValidationError.ofCustomLocation(
                "info",
                "Informasjonen i kravet har endret seg. Vennligst se over på nytt.",
            ),
        ).left()
    } else {
        request.right()
    }
}

fun toRefusjonskravKompakt(krav: RefusjonskravDto) = RefusjonKravKompakt(
    id = krav.id,
    status = krav.status,
    fristForGodkjenning = krav.fristForGodkjenning,
    tiltakstype = krav.tiltakstype,
    gjennomforing = krav.gjennomforing,
    arrangor = krav.arrangor,
    beregning = krav.beregning.let {
        RefusjonKravKompakt.Beregning(
            periodeStart = it.input.periode.start,
            periodeSlutt = it.input.periode.getLastDate(),
            belop = it.output.belop,
        )
    },
)

suspend fun toRefusjonskrav(
    pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    deltakerRepository: DeltakerRepository,
    krav: RefusjonskravDto,
): RefusjonKravAft = when (val beregning = krav.beregning) {
    is RefusjonKravBeregningAft -> {
        val deltakere = deltakerRepository.getAll(krav.gjennomforing.id)

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

        RefusjonKravAft(
            id = krav.id,
            status = krav.status,
            fristForGodkjenning = krav.fristForGodkjenning,
            tiltakstype = krav.tiltakstype,
            gjennomforing = krav.gjennomforing,
            arrangor = krav.arrangor,
            deltakelser = deltakelser,
            beregning = RefusjonKravAft.Beregning(
                periodeStart = beregning.input.periode.start,
                periodeSlutt = beregning.input.periode.getLastDate(),
                antallManedsverk = antallManedsverk,
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
            ),
            betalingsinformasjon = krav.betalingsinformasjon,
        )
    }
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
                description = "Klarte ikke hente informasjon om deltakere i refusjonskravet.",
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

fun refusjonskravJournalpost(
    pdf: ByteArray,
    refusjonskravId: UUID,
    organisasjonsnummer: Organisasjonsnummer,
): Journalpost = Journalpost(
    tittel = "Refusjonskrav",
    journalposttype = "INNGAAENDE",
    avsenderMottaker = Journalpost.AvsenderMottaker(
        id = organisasjonsnummer.value,
        idType = "ORGNR",
        navn = null,
    ),
    bruker = Journalpost.Bruker(
        id = organisasjonsnummer.value,
        idType = "ORGNR",
    ),
    tema = "TIL",
    datoMottatt = LocalDateTime.now().toString(),
    dokumenter = listOf(
        Journalpost.Dokument(
            tittel = "Refusjonskrav",
            dokumentvarianter = listOf(
                Journalpost.Dokument.Dokumentvariant(
                    "PDFA",
                    pdf,
                    "ARKIV",
                ),
            ),
        ),
    ),
    eksternReferanseId = refusjonskravId.toString(),
    journalfoerendeEnhet = "9999", // Automatisk journalføring
    kanal = "NAV_NO", // Påkrevd for INNGAENDE. Se https://confluence.adeo.no/display/BOA/Mottakskanal
    sak = null,
    behandlingstema = null,
)

@Serializable
data class RelevanteForslag(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    val antallRelevanteForslag: Int,
)
