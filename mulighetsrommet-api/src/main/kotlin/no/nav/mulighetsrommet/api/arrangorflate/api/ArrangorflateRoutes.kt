package no.nav.mulighetsrommet.api.arrangorflate.api

import arrow.core.getOrElse
import arrow.core.left
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.pdfgen.*
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.api.toReadableName
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.clamav.ClamAvClient
import no.nav.mulighetsrommet.clamav.Content
import no.nav.mulighetsrommet.clamav.Status
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.unleash.FeatureToggleContext
import no.nav.mulighetsrommet.unleash.UnleashService
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
    val unleashService: UnleashService by inject()

    fun RoutingContext.arrangorTilganger(): List<Organisasjonsnummer>? {
        return call.principal<ArrangorflatePrincipal>()?.organisasjonsnummer
    }

    fun RoutingContext.requireTilgangHosArrangor(organisasjonsnummer: Organisasjonsnummer) = arrangorTilganger()
        ?.find { it == organisasjonsnummer }
        ?: throw StatusException(HttpStatusCode.Forbidden, "Ikke tilgang til bedrift")

    suspend fun resolveArrangor(organisasjonsnummer: Organisasjonsnummer): ArrangorDto {
        return arrangorService.getArrangorOrSyncFromBrreg(organisasjonsnummer)
            .getOrElse {
                when (it) {
                    is BrregError.NotFound -> throw StatusException(
                        HttpStatusCode.BadRequest,
                        "Fant ikke arrangør $organisasjonsnummer i Brreg",
                    )

                    is BrregError.FjernetAvJuridiskeArsaker -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Arrangør $organisasjonsnummer er fjernet av juridiske årsaker",
                    )

                    is BrregError.BadRequest, BrregError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Feil oppsto ved henting av arrangør $organisasjonsnummer fra Brreg",
                    )
                }
            }
    }

    route("/arrangorflate") {
        get("/tilgang-arrangor") {
            val arrangorer = arrangorTilganger()
                ?.map { resolveArrangor(it) }
                ?: throw StatusException(HttpStatusCode.Unauthorized, "Mangler altinn tilgang")

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
                            "Klarte ikke hente kontonummer",
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
                val request = receiveOpprettKravOmUtbetalingRequest(call)

                // Scan vedlegg for virus
                if (clamAvClient.virusScanVedlegg(request.vedlegg).any { it.Result == Status.FOUND }) {
                    throw StatusException(HttpStatusCode.BadRequest, "Virus funnet i minst ett vedlegg")
                }

                UtbetalingValidator.validateOpprettKravOmUtbetaling(request)
                    .onLeft {
                        return@post call.respondWithStatusResponse(ValidationError(errors = it).left())
                    }
                    .onRight {
                        val utbetaling = utbetalingService.opprettUtbetaling(it, Arrangor)
                        call.respondText(utbetaling.id.toString())
                    }
            }

            get("/tilsagn") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                val tilsagn = arrangorFlateService.getTilsagnByOrgnr(orgnr)

                call.respond(tilsagn)
            }
            get("/features") {
                val orgnr = call.parameters.getOrFail("orgnr").let { Organisasjonsnummer(it) }

                requireTilgangHosArrangor(orgnr)

                val feature: String by call.parameters
                val tiltakskoder = call.parameters.getAll("tiltakskoder")
                    ?.map { Tiltakskode.valueOf(it) }
                    ?: emptyList()

                val context = FeatureToggleContext(
                    userId = "",
                    sessionId = call.generateSessionId(),
                    remoteAddress = call.request.origin.remoteAddress,
                    tiltakskoder = tiltakskoder,
                    orgnr = listOf(orgnr),
                )

                val isEnabled = unleashService.isEnabled(feature, context)

                call.respond(isEnabled)
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
                )
                    .onLeft {
                        return@post call.respondWithStatusResponse(ValidationError(errors = it).left())
                    }
                    .onRight {
                        utbetalingService.godkjentAvArrangor(utbetaling.id, it)
                    }

                call.respond(HttpStatusCode.OK)
            }

            get("/utbetalingsdetaljer") {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetaling = arrangorFlateService.getUtbetaling(id)
                    ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
                requireTilgangHosArrangor(utbetaling.arrangor.organisasjonsnummer)

                val arrflateUtbetaling = arrangorFlateService.toArrFlateUtbetaling(utbetaling)
                val pdfContent = pdfClient.getUtbetalingKvittering(
                    utbetaling = UtbetalingPdfDto(
                        status = ArrFlateUtbetalingStatus.toReadableName(arrflateUtbetaling.status),
                        periodeStart = arrflateUtbetaling.periode.start,
                        periodeSlutt = arrflateUtbetaling.periode.slutt.minusDays(1),
                        arrangor = ArrangorPdf(
                            organisasjonsnummer = arrflateUtbetaling.arrangor.organisasjonsnummer.value,
                            navn = arrflateUtbetaling.arrangor.navn,
                        ),
                        godkjentAvArrangorTidspunkt = arrflateUtbetaling.godkjentAvArrangorTidspunkt,
                        createdAt = arrflateUtbetaling.createdAt,
                        gjennomforing = GjennomforingPdf(
                            navn = arrflateUtbetaling.gjennomforing.navn,
                        ),
                        tiltakstype = TiltakstypePdf(
                            navn = arrflateUtbetaling.tiltakstype.navn,
                        ),
                        beregning = when (arrflateUtbetaling.beregning) {
                            is Beregning.Fri -> BeregningPdf(
                                antallManedsverk = null,
                                belop = arrflateUtbetaling.beregning.belop,
                                deltakelser = emptyList(),
                                stengt = emptyList(),
                            )

                            is Beregning.Forhandsgodkjent -> BeregningPdf(
                                antallManedsverk = arrflateUtbetaling.beregning.antallManedsverk,
                                belop = arrflateUtbetaling.beregning.belop,
                                deltakelser = emptyList(),
                                stengt = emptyList(),
                            )
                        },
                        betalingsinformasjon = arrflateUtbetaling.betalingsinformasjon,
                        linjer = arrflateUtbetaling.linjer.map {
                            UtbetalingslinjerPdfDto(
                                id = it.id,
                                tilsagn = it.tilsagn,
                                status = toReadableName(it.status),
                                belop = it.belop,
                                statusSistOppdatert = it.statusSistOppdatert,
                            )
                        },
                    ),
                )

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

private suspend fun receiveOpprettKravOmUtbetalingRequest(call: RoutingCall): OpprettKravOmUtbetalingRequest {
    var gjennomforingId: UUID? = null
    var tilsagnId: UUID? = null
    var periodeStart: String? = null
    var periodeSlutt: String? = null
    var kontonummer: String? = null
    var kidNummer: String? = null
    var belop: Int? = null
    val vedlegg: MutableList<Vedlegg> = mutableListOf()
    val multipart = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 100)

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                when (part.name) {
                    "gjennomforingId" -> gjennomforingId = UUID.fromString(part.value)
                    "tilsagnId" -> tilsagnId = UUID.fromString(part.value)
                    "kontonummer" -> kontonummer = part.value
                    "kidNummer" -> kidNummer = part.value
                    "belop" -> belop = part.value.toInt()
                    "periodeStart" -> periodeStart = part.value
                    "periodeSlutt" -> periodeSlutt = part.value
                }
            }

            is PartData.FileItem -> {
                if (part.name == "vedlegg") {
                    vedlegg.add(
                        Vedlegg(
                            content = Content(
                                contentType = part.contentType.toString(),
                                content = part.provider().toByteArray(),
                            ),
                            filename = part.originalFileName ?: "ukjent.pdf",
                        ),
                    )
                }
            }

            else -> {}
        }

        part.dispose()
    }

    val validatedVedlegg = vedlegg.map { v ->
        // Optionally validate file type and size here
        val fileName = v.filename
        val contentType = v.content.contentType

        require(contentType.equals("application/pdf", ignoreCase = true)) {
            "Vedlegg $fileName er ikke en PDF"
        }

        v
    }

    return OpprettKravOmUtbetalingRequest(
        gjennomforingId = requireNotNull(gjennomforingId) { "Mangler gjennomforingId" },
        tilsagnId = requireNotNull(tilsagnId) { "Mangler tilsagnId" },
        periodeStart = requireNotNull(periodeStart) { "Mangler periodeStart" },
        periodeSlutt = requireNotNull(periodeSlutt) { "Mangler periodeSlutt" },
        kontonummer = requireNotNull(kontonummer) { "Mangler kontonummer" },
        kidNummer = kidNummer,
        belop = belop ?: 0,
        tilskuddstype = Tilskuddstype.TILTAK_INVESTERINGER,
        vedlegg = validatedVedlegg,
    )
}

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
    val digest: String,
    val kid: String?,
)

@Serializable
data class RelevanteForslag(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    val antallRelevanteForslag: Int,
)

@Serializable
data class OpprettKravOmUtbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    val periodeStart: String,
    val periodeSlutt: String,
    val kontonummer: String,
    val kidNummer: String? = null,
    val belop: Int,
    val vedlegg: List<Vedlegg>,
    val tilskuddstype: Tilskuddstype,
)

private fun ApplicationCall.generateSessionId(): String {
    val uuid = UUID.randomUUID()
    val sessionId =
        java.lang.Long.toHexString(uuid.mostSignificantBits) + java.lang.Long.toHexString(uuid.leastSignificantBits)
    val cookie = Cookie(name = "UNLEASH_SESSION_ID", value = sessionId, path = "/", maxAge = -1)
    this.response.cookies.append(cookie)
    return sessionId
}
