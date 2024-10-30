package no.nav.mulighetsrommet.api.okonomi.refusjon

import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
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
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.domain.dto.DeltakerDto
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDto
import no.nav.mulighetsrommet.api.okonomi.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.services.ArrangorService
import no.nav.mulighetsrommet.domain.dto.Kid
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.pdfgen.core.Environment
import no.nav.pdfgen.core.PDFGenCore
import no.nav.pdfgen.core.pdf.createHtmlFromTemplateData
import no.nav.pdfgen.core.pdf.createPDFA
import org.koin.ktor.ext.inject
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
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

    suspend fun <T : Any> PipelineContext<T, ApplicationCall>.arrangorerMedTilgang(): List<UUID> {
        return call.principal<ArrangorflatePrincipal>()
            ?.organisasjonsnummer
            ?.map {
                arrangorService.getOrSyncArrangorFromBrreg(it)
                    .getOrElse {
                        throw StatusException(HttpStatusCode.InternalServerError, "Feil ved henting av arrangor_id")
                    }
                    .id
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
        route("/refusjonskrav") {
            get {
                val arrangorIds = arrangorerMedTilgang()

                val krav = refusjonskrav.getByArrangorIds(arrangorIds)
                    .map { toRefusjonskravKompakt(it) }

                call.respond(krav)
            }

            get("/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val oppsummering = toRefusjonskrav(pdl, deltakerRepository, krav)

                call.respond(oppsummering)
            }

            post("/{id}/godkjenn-refusjon") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)
                val request = call.receive<SetRefusjonKravBetalingsinformasjonRequest>()

                refusjonskrav.setGodkjentAvArrangor(id, LocalDateTime.now())
                refusjonskrav.setBetalingsInformasjon(
                    id,
                    request.kontonummer,
                    request.kid,
                )

                call.respond(HttpStatusCode.OK)
            }

            get("/{id}/kvittering") {
                val id = call.parameters.getOrFail<UUID>("id")
                val html = createHtmlFromTemplateData("refusjon-kvittering", "refusjon").toString()
                val pdfBytes: ByteArray = createPDFA(html)

                call.response.headers.append(
                    "Content-Disposition",
                    "attachment; filename=\"kvittering.pdf\"",
                )
                call.respondBytes(pdfBytes, contentType = ContentType.Application.Pdf)
            }

            get("/{id}/tilsagn") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                when (krav.beregning) {
                    is RefusjonKravBeregningAft -> {
                        val tilsagn = tilsagnService.getArrangorflateTilsagnTilRefusjon(
                            gjennomforingId = krav.gjennomforing.id,
                            periodeStart = krav.beregning.input.periodeStart.toLocalDate(),
                            periodeSlutt = krav.beregning.input.periodeSlutt.toLocalDate(),
                        )
                        call.respond(tilsagn)
                    }
                }
            }
        }

        route("/tilsagn") {
            get {
                call.respond(tilsagnService.getAllArrangorflateTilsagn(arrangorerMedTilgang()))
            }

            get("/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")

                val tilsagn = tilsagnService.getArrangorflateTilsagn(id)
                    ?: throw NotFoundException("Fant ikke tilsagn")
                requireTilgangHosArrangor(tilsagn.arrangor.organisasjonsnummer)

                call.respond(tilsagn)
            }
        }
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
            periodeStart = it.input.periodeStart,
            periodeSlutt = it.input.periodeSlutt,
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
            RefusjonKravDeltakelse(
                id = id,
                perioder = deltakelse.perioder,
                manedsverk = manedsverk,
                startDato = deltaker.startDato,
                sluttDato = deltaker.startDato,
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
                periodeStart = beregning.input.periodeStart,
                periodeSlutt = beregning.input.periodeSlutt,
                antallManedsverk = antallManedsverk,
                belop = beregning.output.belop,
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

private fun toRefusjonskravPerson(person: HentPersonBolkResponse.Person) =
    when (person.adressebeskyttelse.gradering) {
        PdlGradering.UGRADERT -> {
            val navn = person.navn.firstOrNull()?.let { navn ->
                val fornavnOgMellomnavn = listOfNotNull(navn.fornavn, navn.mellomnavn)
                    .joinToString(" ")
                    .takeIf { it.isNotEmpty() }
                listOfNotNull(navn.etternavn, fornavnOgMellomnavn).joinToString(", ")
            }
            val foedselsdato = person.foedselsdato.firstOrNull()
            RefusjonKravDeltakelse.Person(
                navn = navn ?: "Mangler navn",
                fodselsaar = foedselsdato?.foedselsaar,
                fodselsdato = foedselsdato?.foedselsdato,
            )
        }

        else -> RefusjonKravDeltakelse.Person(
            navn = "Adressebeskyttet",
            fodselsaar = null,
            fodselsdato = null,
        )
    }

@Serializable
data class RefusjonKravKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: RefusjonskravStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fristForGodkjenning: LocalDateTime,
    val tiltakstype: RefusjonskravDto.Tiltakstype,
    val gjennomforing: RefusjonskravDto.Gjennomforing,
    val arrangor: RefusjonskravDto.Arrangor,
    val beregning: Beregning,
) {

    @Serializable
    data class Beregning(
        @Serializable(with = LocalDateTimeSerializer::class)
        val periodeStart: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        val periodeSlutt: LocalDateTime,
        val belop: Int,
    )
}

@Serializable
data class RefusjonKravAft(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: RefusjonskravStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fristForGodkjenning: LocalDateTime,
    val tiltakstype: RefusjonskravDto.Tiltakstype,
    val gjennomforing: RefusjonskravDto.Gjennomforing,
    val arrangor: RefusjonskravDto.Arrangor,
    val deltakelser: List<RefusjonKravDeltakelse>,
    val beregning: Beregning,
    val betalingsinformasjon: RefusjonskravDto.Betalingsinformasjon,
) {
    @Serializable
    data class Beregning(
        @Serializable(with = LocalDateTimeSerializer::class)
        val periodeStart: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        val periodeSlutt: LocalDateTime,
        val antallManedsverk: Double,
        val belop: Int,
    )
}

@Serializable
data class RefusjonKravDeltakelse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val perioder: List<DeltakelsePeriode>,
    val manedsverk: Double,
    val person: Person?,
    val veileder: String?,
) {
    @Serializable
    data class Person(
        val navn: String,
        @Serializable(with = LocalDateSerializer::class)
        val fodselsdato: LocalDate?,
        val fodselsaar: Int?,
    )
}

@Serializable
data class SetRefusjonKravBetalingsinformasjonRequest(
    val kontonummer: Kontonummer,
    val kid: Kid?,
)
