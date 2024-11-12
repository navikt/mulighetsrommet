package no.nav.mulighetsrommet.api.refusjon

import arrow.core.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.refusjon.model.*
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.ArrangorflateTilsagn
import no.nav.mulighetsrommet.domain.dto.Kid
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.pdfgen.core.pdf.createPDFA
import org.koin.ktor.ext.inject
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.*

fun Route.arrangorflateRoutes() {
    VeraGreenfieldFoundryProvider.initialise()
    PDFGenCore.init(Environment())

    val tilsagnService: TilsagnService by inject()
    val arrangorService: ArrangorService by inject()
    val refusjonskrav: RefusjonskravRepository by inject()
    val deltakerRepository: DeltakerRepository by inject()

    val pdl: HentAdressebeskyttetPersonBolkPdlQuery by inject()

    suspend fun <T : Any> PipelineContext<T, ApplicationCall>.arrangorerMedTilgang(): List<ArrangorDto> {
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

    fun <T : Any> PipelineContext<T, ApplicationCall>.requireTilgangHosArrangor(organisasjonsnummer: Organisasjonsnummer) {
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

            post("/godkjenn-refusjon") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")

                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val request = call.receive<GodkjennRefusjonskravAft>()

                val result = validerGodkjennRefusjonskrav(request, krav)
                    .mapLeft { BadRequest(errors = it) }
                    .map { (betalingsinformasjon) ->
                        refusjonskrav.setGodkjentAvArrangor(id, LocalDateTime.now())
                        refusjonskrav.setBetalingsInformasjon(
                            id,
                            betalingsinformasjon.kontonummer,
                            betalingsinformasjon.kid,
                        )
                    }

                call.respondWithStatusResponse(result)
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

                val oppsummering = toRefusjonskrav(pdl, deltakerRepository, krav)
                val mapper = ObjectMapper().apply {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    registerKotlinModule()
                }
                val dto = RefusjonKravKvitteringDto(oppsummering, tilsagn)
                val jsonNode: JsonNode = mapper.valueToTree<JsonNode>(dto)
                val pdfBytes: ByteArray = createPDFA("refusjon-kvittering", "refusjon", jsonNode)
                    ?: throw Exception("Kunne ikke generere PDF")

                call.response.headers.append(
                    "Content-Disposition",
                    "attachment; filename=\"kvittering.pdf\"",
                )
                call.respondBytes(pdfBytes, contentType = ContentType.Application.Pdf)
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

fun validerGodkjennRefusjonskrav(
    request: GodkjennRefusjonskravAft,
    krav: RefusjonskravDto,
): Either<List<ValidationError>, GodkjennRefusjonskravAft> {
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
) = when (val beregning = krav.beregning) {
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
data class RefusjonKravKvitteringDto(
    val refusjon: RefusjonKravAft,
    val tilsagn: List<ArrangorflateTilsagn>,
)

// Kan bli gjort om til en sealed class for andre etterhvert hvis det trengs
@Serializable
data class GodkjennRefusjonskravAft(
    val betalingsinformasjon: Betalingsinformasjon,
    val digest: String,
) {
    @Serializable
    data class Betalingsinformasjon(
        val kontonummer: Kontonummer,
        val kid: Kid?,
    )
}
