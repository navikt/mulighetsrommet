package no.nav.mulighetsrommet.api.okonomi.refusjon

import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDto
import no.nav.mulighetsrommet.api.okonomi.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.services.ArrangorService
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun Route.arrangorflateRoutes() {
    VeraGreenfieldFoundryProvider.initialise()
    PDFGenCore.init(Environment())

    val tilsagnService: TilsagnService by inject()
    val arrangorService: ArrangorService by inject()
    val refusjonskrav: RefusjonskravRepository by inject()

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
                    .map {
                        // TODO egen listemodell som er generell på tvers av beregningstype?
                        toRefusjonKravOppsummering(it)
                    }

                call.respond(krav)
            }

            get("/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                val oppsummering = toRefusjonKravOppsummering(krav)

                call.respond(oppsummering)
            }

            post("/{id}/godkjenn-refusjon") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id)
                    ?: throw NotFoundException("Fant ikke refusjonskrav med id=$id")
                requireTilgangHosArrangor(krav.arrangor.organisasjonsnummer)

                refusjonskrav.setGodkjentAvArrangor(id, LocalDateTime.now())

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

private fun toRefusjonKravOppsummering(krav: RefusjonskravDto) = when (val beregning = krav.beregning) {
    is RefusjonKravBeregningAft -> {
        val perioder = beregning.input.deltakelser.associateBy { it.deltakelseId }
        val manedsverk = beregning.output.deltakelser.associateBy { it.deltakelseId }

        val deltakelser = perioder.map { (id, perioder) ->
            RefusjonKravDeltakelse(
                id = id,
                perioder = perioder.perioder,
                manedsverk = manedsverk.getValue(id).manedsverk,
                // TODO data om deltaker
                norskIdent = NorskIdent("12345678910"),
                navn = "TODO TODOESEN",
                startDato = null,
                sluttDato = null,
                // TODO data om veileder hos arrangør
                veileder = null,
            )
        }

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
                antallManedsverk = deltakelser.sumOf { it.manedsverk },
                belop = beregning.output.belop,
            ),
        )
    }
}

@Serializable
@SerialName("AFT")
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
    val norskIdent: NorskIdent,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val perioder: List<DeltakelsePeriode>,
    val manedsverk: Double,
    val veileder: String?,
)
