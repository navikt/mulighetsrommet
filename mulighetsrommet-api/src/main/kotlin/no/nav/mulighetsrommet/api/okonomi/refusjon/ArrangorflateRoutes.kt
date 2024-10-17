package no.nav.mulighetsrommet.api.okonomi.refusjon

import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.api.okonomi.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDto
import no.nav.mulighetsrommet.api.okonomi.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.plugins.getPid
import no.nav.mulighetsrommet.api.services.ArrangorService
import no.nav.mulighetsrommet.domain.dto.NorskIdent
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
    val altinnRettigheterService: AltinnRettigheterService by inject()
    val arrangorService: ArrangorService by inject()
    val refusjonskrav: RefusjonskravRepository by inject()

    route("/arrangorflate") {
        route("/refusjonskrav") {
            get {
                val rettigheter = altinnRettigheterService.getRettigheter(getPid())
                if (rettigheter.isEmpty()) {
                    return@get call.respond(HttpStatusCode.Forbidden)
                }

                val arrangorer = rettigheter.map {
                    arrangorService.getOrSyncArrangorFromBrreg(it.organisasjonsnummer.value)
                        .getOrElse {
                            throw StatusException(HttpStatusCode.InternalServerError, "Feil ved henting av arrangor_id")
                        }
                }

                val krav = refusjonskrav.getByArrangorIds(arrangorer.map { it.id })
                    .map {
                        // TODO egen listemodell som er generell på tvers av beregningstype?
                        toRefusjonKravOppsummering(it)
                    }

                call.respond(krav)
            }

            get("/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")

                val krav = refusjonskrav.get(id) ?: throw NotFoundException("Fant ikke refusjonskra med id=$id")

                val oppsummering = toRefusjonKravOppsummering(krav)

                call.respond(oppsummering)
            }

            post("/{id}/godkjenn-refusjon") {
                val id = call.parameters.getOrFail<UUID>("id")

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
                val rettigheter = altinnRettigheterService.getRettigheter(getPid())
                if (rettigheter.isEmpty()) {
                    return@get call.respond(HttpStatusCode.Forbidden)
                }
                val arrangorer = rettigheter.map {
                    arrangorService.getOrSyncArrangorFromBrreg(it.organisasjonsnummer.value)
                        .getOrElse {
                            throw StatusException(
                                HttpStatusCode.InternalServerError,
                                "Feil ved henting av arrangor_id",
                            )
                        }
                }

                val tilsagn = tilsagnService.getAllArrangorflateTilsagn(arrangorer.map { it.id })
                call.respond(tilsagn)
            }

            get("/{id}") {
                val rettigheter = altinnRettigheterService.getRettigheter(getPid())
                if (rettigheter.isEmpty()) {
                    return@get call.respond(HttpStatusCode.Forbidden)
                }
                val id = call.parameters.getOrFail<UUID>("id")

                val tilsagn = tilsagnService.getArrangorflateTilsagn(id)
                    ?: throw NotFoundException("Fant ikke tilsagn")

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
