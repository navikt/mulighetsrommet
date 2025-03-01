package no.nav.mulighetsrommet.api.arrangorflate

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrFlateUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.model.Beregning
import no.nav.mulighetsrommet.api.arrangorflate.model.UtbetalingDeltakelse
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.utbetaling.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.HentPersonBolkResponse
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAft
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

fun Route.arrangorflateRoutes() {
    val tilsagnService: TilsagnService by inject()
    val arrangorService: ArrangorService by inject()
    val utbetalingService: UtbetalingService by inject()
    val pdl: HentAdressebeskyttetPersonBolkPdlQuery by inject()
    val db: ApiDatabase by inject()
    val pdfClient: PdfGenClient by inject()
    val arrangorFlateService: ArrangorFlateService by inject()

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
            get("/utbetaling") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                val utbetalinger = arrangorFlateService.getUtbetalinger(orgnr)

                call.respond(utbetalinger)
            }

            get("/tilsagn") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                val tilsagn = arrangorFlateService.getAlleTilsagnForOrganisasjon(orgnr)

                call.respond(tilsagn)
            }
        }

        route("/utbetaling/{id}") {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetaling = arrangorFlateService.getUtbetaling(id)

                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                val oppsummering = toArrFlateUtbetaling(db, pdl, utbetaling)

                call.respond(oppsummering)
            }

            get("/relevante-forslag") {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetaling = arrangorFlateService.getUtbetaling(id)

                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                val forslagByDeltakerId = arrangorFlateService.getDeltakerforslagByGjennomforing(utbetaling.gjennomforing.id)

                val relevanteForslag = arrangorFlateService.getRelevanteForslag(forslagByDeltakerId, utbetaling)

                call.respond(relevanteForslag)
            }

            post("/godkjenn-utbetaling") {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<GodkjennUtbetaling>()

                val utbetaling = arrangorFlateService.getUtbetaling(id)

                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)
                val forslagByDeltakerId = arrangorFlateService.getDeltakerforslagByGjennomforing(utbetaling.gjennomforing.id)

                UtbetalingValidator.validerGodkjennUtbetaling(
                    request,
                    utbetaling,
                    forslagByDeltakerId,
                ).onLeft {
                    return@post call.respondWithStatusResponse(ValidationError(errors = it).left())
                }

                utbetalingService.godkjentAvArrangor(utbetaling.id, request)
                call.respond(HttpStatusCode.OK)
            }

            get("/kvittering") {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetaling = arrangorFlateService.getUtbetaling(id)

                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                val tilsagn = tilsagnService.getArrangorflateTilsagnTilUtbetaling(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periode = utbetaling.periode,
                )
                val utbetalingAft = toArrFlateUtbetaling(db, pdl, utbetaling)
                val pdfContent = pdfClient.getUtbetalingKvittering(utbetalingAft, tilsagn)

                call.response.headers.append(
                    "Content-Disposition",
                    "attachment; filename=\"kvittering.pdf\"",
                )

                call.respondBytes(pdfContent, contentType = ContentType.Application.Pdf)
            }

            get("/tilsagn") {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetaling = arrangorFlateService.getUtbetaling(id)

                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                val tilsagn = tilsagnService.getArrangorflateTilsagnTilUtbetaling(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periode = utbetaling.periode,
                )

                call.respond(tilsagn)
            }
        }

        route("/tilsagn/{id}") {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val tilsagn = arrangorFlateService.getTilsagn(id)

                requireTilgangHosArrangor(tilsagn.arrangor.organisasjonsnummer)

                call.respond(tilsagn)
            }
        }
    }
}

fun DeltakerForslag.relevantForDeltakelse(
    utbetaling: UtbetalingDto,
): Boolean = when (utbetaling.beregning) {
    is UtbetalingBeregningAft -> this.relevantForDeltakelse(utbetaling.beregning)
    is UtbetalingBeregningFri -> false
}

fun DeltakerForslag.relevantForDeltakelse(
    beregning: UtbetalingBeregningAft,
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

suspend fun toArrFlateUtbetaling(
    db: ApiDatabase,
    pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    utbetaling: UtbetalingDto,
): ArrFlateUtbetaling = when (val beregning = utbetaling.beregning) {
    is UtbetalingBeregningAft -> {
        val deltakere = db.session { queries.deltaker.getAll(gjennomforingId = utbetaling.gjennomforing.id) }

        val deltakereById = deltakere.associateBy { it.id }
        val personerByNorskIdent: Map<NorskIdent, UtbetalingDeltakelse.Person> = getPersoner(pdl, deltakere)
        val perioderById = beregning.input.deltakelser.associateBy { it.deltakelseId }
        val manedsverkById = beregning.output.deltakelser.associateBy { it.deltakelseId }

        val deltakelser = perioderById.map { (id, deltakelse) ->
            val deltaker = deltakereById.getValue(id)
            val manedsverk = manedsverkById.getValue(id).manedsverk
            val person = personerByNorskIdent[deltaker.norskIdent]

            val forstePeriode = deltakelse.perioder.first()
            val sistePeriode = deltakelse.perioder.last()

            UtbetalingDeltakelse(
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

        ArrFlateUtbetaling(
            id = utbetaling.id,
            status = ArrFlateUtbetaling.Status.fromUtbetalingStatus(utbetaling.status),
            fristForGodkjenning = utbetaling.fristForGodkjenning,
            tiltakstype = utbetaling.tiltakstype,
            gjennomforing = utbetaling.gjennomforing,
            arrangor = utbetaling.arrangor,
            periodeStart = utbetaling.periode.start,
            periodeSlutt = utbetaling.periode.getLastInclusiveDate(),
            beregning = Beregning.Forhandsgodkjent(
                antallManedsverk = antallManedsverk,
                belop = beregning.output.belop,
                digest = beregning.getDigest(),
                deltakelser = deltakelser,
            ),
            betalingsinformasjon = utbetaling.betalingsinformasjon,
        )
    }

    is UtbetalingBeregningFri -> ArrFlateUtbetaling(
        id = utbetaling.id,
        status = ArrFlateUtbetaling.Status.fromUtbetalingStatus(utbetaling.status),
        fristForGodkjenning = utbetaling.fristForGodkjenning,
        tiltakstype = utbetaling.tiltakstype,
        gjennomforing = utbetaling.gjennomforing,
        arrangor = utbetaling.arrangor,
        periodeStart = utbetaling.periode.start,
        periodeSlutt = utbetaling.periode.getLastInclusiveDate(),
        beregning = Beregning.Fri(
            belop = beregning.output.belop,
            digest = beregning.getDigest(),
        ),
        betalingsinformasjon = utbetaling.betalingsinformasjon,
    )
}

private suspend fun getPersoner(
    pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    deltakere: List<DeltakerDto>,
): Map<NorskIdent, UtbetalingDeltakelse.Person> {
    val identer = deltakere
        .mapNotNull { deltaker -> deltaker.norskIdent?.value?.let { PdlIdent(it) } }
        .toNonEmptySetOrNull()
        ?: return mapOf()

    return pdl.hentPersonBolk(identer)
        .map {
            buildMap {
                it.entries.forEach { (ident, person) ->
                    val utbetalingPerson = toUtbetalingPerson(person)
                    put(NorskIdent(ident.value), utbetalingPerson)
                }
            }
        }
        .getOrElse {
            throw StatusException(
                status = HttpStatusCode.InternalServerError,
                detail = "Klarte ikke hente informasjon om deltakere i utbetalingen",
            )
        }
}

private fun toUtbetalingPerson(person: HentPersonBolkResponse.Person): UtbetalingDeltakelse.Person {
    val gradering = person.adressebeskyttelse.firstOrNull()?.gradering ?: PdlGradering.UGRADERT
    return when (gradering) {
        PdlGradering.UGRADERT -> {
            val navn = person.navn.first().let { navn ->
                val fornavnOgMellomnavn = listOfNotNull(navn.fornavn, navn.mellomnavn).joinToString(" ")
                listOf(navn.etternavn, fornavnOgMellomnavn).joinToString(", ")
            }
            val foedselsdato = person.foedselsdato.first()
            UtbetalingDeltakelse.Person(
                navn = navn,
                fodselsaar = foedselsdato.foedselsaar,
                fodselsdato = foedselsdato.foedselsdato,
            )
        }

        else -> UtbetalingDeltakelse.Person(
            navn = "Adressebeskyttet",
            fodselsaar = null,
            fodselsdato = null,
        )
    }
}

@Serializable
data class GodkjennUtbetaling(
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
