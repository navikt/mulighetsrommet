package no.nav.mulighetsrommet.api.arrangorflate.api

import arrow.core.getOrElse
import arrow.core.left
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.clamav.ClamAvClient
import no.nav.mulighetsrommet.clamav.Content
import no.nav.mulighetsrommet.clamav.Status
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.arrangorflateRoutes() {
    val arrangorService: ArrangorService by inject()
    val utbetalingService: UtbetalingService by inject()
    val pdfClient: PdfGenClient by inject()
    val arrangorFlateService: ArrangorFlateService by inject()
    val clamAvClient: ClamAvClient by inject()

    fun RoutingContext.arrangorTilganger(): List<Organisasjonsnummer>? {
        return call.principal<ArrangorflatePrincipal>()?.organisasjonsnummer
    }

    fun RoutingContext.requireTilgangHosArrangor(organisasjonsnummer: Organisasjonsnummer) = arrangorTilganger()
        ?.find { it == organisasjonsnummer }
        ?: throw StatusException(HttpStatusCode.Forbidden, "Ikke tilgang til bedrift")

    route("/arrangorflate") {
        get("/tilgang-arrangor") {
            val arrangorer = arrangorTilganger()
                ?.map {
                    arrangorService.getArrangorOrSyncFromBrreg(it).getOrElse {
                        throw StatusException(HttpStatusCode.InternalServerError, "Feil ved henting av arrangor_id")
                    }
                } ?: throw StatusException(HttpStatusCode.Unauthorized, "Mangler altinn tilgang")

            call.respond(arrangorer)
        }

        route("/arrangor/{orgnr}") {
            get("/kontonummer") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                arrangorFlateService.getKontonummer(orgnr)
                    .onLeft {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Kunne ikke synkronisere kontonummer",
                        )
                    }
                    .onRight { call.respond(it) }
            }

            get("/gjennomforing") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                call.respond(arrangorFlateService.getGjennomforinger(orgnr))
            }

            get("/utbetaling") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                val utbetalinger = arrangorFlateService.getUtbetalinger(orgnr)

                call.respond(utbetalinger)
            }

            post("/utbetaling") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)
                val formData = getFormData(call)

                // Scan vedlegg for virus
                if (clamAvClient.virusScanVedlegg(formData.vedlegg).any { it.Result == Status.FOUND }) {
                    throw BadRequestException("Virus funnet i minst ett vedlegg")
                }

                val request = ArrangorflateManuellUtbetalingRequest(
                    gjennomforingId = formData.gjennomforingId,
                    periodeStart = formData.periodeStart,
                    periodeSlutt = formData.periodeSlutt,
                    beskrivelse = formData.beskrivelse,
                    kontonummer = formData.kontonummer,
                    kidNummer = formData.kidNummer,
                    belop = formData.belop,
                    vedlegg = formData.vedlegg,
                    tilskuddstype = formData.tilskuddstype,
                )

                UtbetalingValidator.validateArrangorflateManuellUtbetalingskrav(request)
                    .onLeft {
                        return@post call.respondWithStatusResponse(ValidationError(errors = it).left())
                    }
                    .onRight {
                        call.respondText(utbetalingService.opprettManuellUtbetaling(it, Arrangor).toString())
                    }
            }

            get("/tilsagn") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                val tilsagn = arrangorFlateService.getTilsagnByOrgnr(orgnr)

                call.respond(tilsagn)
            }
        }

        route("/utbetaling/{id}") {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetaling = arrangorFlateService.getUtbetaling(id)
                    ?: throw NotFoundException("Fant ikke utbetaling med id=$id")

                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                val oppsummering = arrangorFlateService.toArrFlateUtbetaling(utbetaling)

                call.respond(oppsummering)
            }

            get("/relevante-forslag") {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetaling = arrangorFlateService.getUtbetaling(id)
                    ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                call.respond(arrangorFlateService.getRelevanteForslag(utbetaling))
            }

            post("/godkjenn") {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<GodkjennUtbetaling>()

                val utbetaling = arrangorFlateService.getUtbetaling(id)
                    ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)
                val relevanteForslag = arrangorFlateService.getRelevanteForslag(utbetaling)

                UtbetalingValidator.validerGodkjennUtbetaling(
                    request,
                    utbetaling,
                    relevanteForslag,
                ).onLeft {
                    return@post call.respondWithStatusResponse(ValidationError(errors = it).left())
                }

                utbetalingService.godkjentAvArrangor(utbetaling.id, request)
                call.respond(HttpStatusCode.OK)
            }

            get("/kvittering") {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetaling = arrangorFlateService.getUtbetaling(id)
                    ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                val tilsagn = arrangorFlateService.getArrangorflateTilsagnTilUtbetaling(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periode = utbetaling.periode,
                )
                val utbetalingAft = arrangorFlateService.toArrFlateUtbetaling(utbetaling)
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
                    ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                val tilsagn = arrangorFlateService.getArrangorflateTilsagnTilUtbetaling(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periode = utbetaling.periode,
                )

                call.respond(tilsagn)
            }

            get("/sync-kontonummer") {
                val id = call.parameters.getOrFail<UUID>("id")
                val utbetaling = arrangorFlateService.getUtbetaling(id)
                    ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                arrangorFlateService.synkroniserKontonummer(utbetaling).map { call.respond(it) }.mapLeft {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Kunne ikke synkronisere kontonummer",
                    )
                }
            }
        }

        route("/tilsagn/{id}") {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val tilsagn = arrangorFlateService.getTilsagn(id)
                    ?: throw NotFoundException("Fant ikke tilsagn")
                requireTilgangHosArrangor(tilsagn.arrangor.organisasjonsnummer)

                call.respond(tilsagn)
            }
        }
    }
}

private suspend fun generateVedlegg(part: PartData.FileItem) = Vedlegg(
    content = Content(
        contentType = part.contentType.toString(),
        content = part.provider().toByteArray(),
    ),
    description = part.originalFileName ?: "ukjent.pdf",
)

private suspend fun getFormData(call: RoutingCall): FormData {
    var gjennomforingId: UUID? = null
    var periodeStart: String? = null
    var periodeSlutt: String? = null
    var beskrivelse: String? = null
    var kontonummer: String? = null
    var kidNummer: String? = null
    var belop: Int? = null
    var tilskuddstype: Tilskuddstype? = null
    var vedlegg: MutableList<Vedlegg> = mutableListOf()
    val multipart = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 100)

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                when (part.name) {
                    "gjennomforingId" -> gjennomforingId = UUID.fromString(part.value)
                    "beskrivelse" -> beskrivelse = part.value
                    "kontonummer" -> kontonummer = part.value
                    "kidNummer" -> kidNummer = part.value
                    "belop" -> belop = part.value.toInt()
                    "periodeStart" -> periodeStart = part.value
                    "periodeSlutt" -> periodeSlutt = part.value
                    "tilskuddstype" -> tilskuddstype = Tilskuddstype.valueOf(part.value)
                }
            }

            is PartData.FileItem -> {
                if (part.name == "vedlegg") {
                    vedlegg.add(
                        generateVedlegg(part),
                    )
                }
            }

            else -> {}
        }

        part.dispose()
    }

    requireNotNull(gjennomforingId) { "Mangler gjennomforingId" }
    requireNotNull(beskrivelse) { "Mangler beskrivelse" }
    requireNotNull(kontonummer) { "Mangler kontonummer" }
    requireNotNull(periodeStart) { "Mangler periodeStart" }
    requireNotNull(periodeSlutt) { "Mangler periodeSlutt" }
    requireNotNull(vedlegg) { "Mangler vedlegg" }
    requireNotNull(tilskuddstype) { "Mangler tilskuddstype" }

    val validatedVedlegg = vedlegg.map { v ->
        // Optionally validate file type and size here
        val fileName = v.description
        val contentType = v.content.contentType

        if (!contentType.equals("application/pdf", ignoreCase = true)) {
            throw IllegalArgumentException("Vedlegg $fileName er ikke en PDF")
        }

        v
    }

    return FormData(
        gjennomforingId = gjennomforingId,
        periodeStart = periodeStart,
        periodeSlutt = periodeSlutt,
        beskrivelse = beskrivelse,
        kontonummer = kontonummer,
        kidNummer = kidNummer,
        belop = belop ?: 0,
        tilskuddstype = tilskuddstype,
        vedlegg = validatedVedlegg,
    )
}

data class FormData(
    val gjennomforingId: UUID,
    val periodeStart: String,
    val periodeSlutt: String,
    val beskrivelse: String,
    val kontonummer: String,
    val kidNummer: String?,
    val belop: Int,
    val tilskuddstype: Tilskuddstype,
    val vedlegg: List<Vedlegg>,
)

@Serializable
data class ArrangorflateGjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
)

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

@Serializable
data class ArrangorflateManuellUtbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val periodeStart: String,
    val periodeSlutt: String,
    val beskrivelse: String,
    val kontonummer: String,
    val kidNummer: String? = null,
    val belop: Int,
    val vedlegg: List<Vedlegg>,
    val tilskuddstype: Tilskuddstype,
)
