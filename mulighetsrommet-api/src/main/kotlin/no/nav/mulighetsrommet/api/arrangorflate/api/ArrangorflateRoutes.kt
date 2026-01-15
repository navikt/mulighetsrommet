package no.nav.mulighetsrommet.api.arrangorflate.api

import arrow.core.getOrElse
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.application
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import io.ktor.utils.io.toByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.altinn.AltinnError
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorflateService
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingStatus.AVBRUTT
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingStatus.DELVIS_UTBETALT
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingStatus.KREVER_ENDRING
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingStatus.UTBETALT
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.mapper.UbetalingToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.clamav.ClamAvClient
import no.nav.mulighetsrommet.clamav.Content
import no.nav.mulighetsrommet.clamav.Status
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

suspend fun RoutingContext.respondWithManglerTilgangHosArrangor() = call.respondWithProblemDetail(
    Forbidden(
        detail = """
                        Du mangler tilgang til utbetalingsløsningen. Tilgang delegeres i Altinn som en
                        enkeltrettighet av din arbeidsgiver. Det er enkeltrettigheten
                        “Be om utbetaling - Nav Arbeidsmarkedstiltak” du må få via Altinn. Når enkeltrettigheten
                        er delegert i Altinn kan du laste siden på nytt og få tilgang.
                    """,
    ),
)

suspend inline fun RoutingContext.orgnrTilganger(
    altinnRettigheterService: AltinnRettigheterService,
): List<Organisasjonsnummer> {
    return call.principal<ArrangorflatePrincipal>()?.norskIdent?.let {
        altinnRettigheterService.getRettigheter(it)
            .getOrElse {
                when (it) {
                    AltinnError.Error ->
                        call.respondWithProblemDetail(
                            InternalServerError("Klarte ikke få kontakt med Altinn. Vennligst prøv igjen senere"),
                        )

                    AltinnError.ForMangeTilganger ->
                        call.respondWithProblemDetail(
                            InternalServerError("For mange Altinn tilganger. Vennligst ta kontakt med Nav"),
                        )
                }
                emptyList()
            }
            .filter { AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING in it.rettigheter }
            .map { it.organisasjonsnummer }
    } ?: emptyList()
}

suspend fun RoutingContext.requireTilgangHosArrangor(
    altinnRettigheterService: AltinnRettigheterService,
    organisasjonsnummer: Organisasjonsnummer,
) = orgnrTilganger(altinnRettigheterService)
    .find { it == organisasjonsnummer }
    ?: throw StatusException(HttpStatusCode.Forbidden, "Ikke tilgang til bedrift")

fun Route.arrangorflateRoutes(config: AppConfig) {
    val db: ApiDatabase by inject()
    val utbetalingService: UtbetalingService by inject()
    val pdfClient: PdfGenClient by inject()
    val arrangorFlateService: ArrangorflateService by inject()
    val clamAvClient: ClamAvClient by inject()
    val altinnRettigheterService: AltinnRettigheterService by inject()
    val genererUtbetalingService: GenererUtbetalingService by inject()

    fun RoutingContext.getTilsagnOrRespondNotFound(): ArrangorflateTilsagnDto {
        val id: UUID by call.parameters
        return arrangorFlateService.getTilsagn(id) ?: throw NotFoundException("Fant ikke tilsagn med id=$id")
    }

    fun RoutingContext.getUtbetalingOrRespondNotFound(): Utbetaling {
        val id: UUID by call.parameters
        return arrangorFlateService.getUtbetaling(id) ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
    }

    arrangorflateRoutesOpprettKrav(config.okonomi)

    route("/tilsagn") {
        get(
            {
                description = "Hent oversikt over tilsagn for alle arrangører brukeren har tilgang til"
                tags = setOf("Arrangorflate")
                operationId = "getArrangorflateTilsagnOversikt"
                response {
                    code(HttpStatusCode.OK) {
                        description = "Utbetalinger i tabellformat"
                        body<ArrangorflateTilsagnOversikt>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            },
        ) {
            val tilganger = orgnrTilganger(altinnRettigheterService)
            if (tilganger.isEmpty()) {
                respondWithManglerTilgangHosArrangor()
                return@get
            }
            val tilsagn =
                arrangorFlateService.getTilsagn(tilganger.toSet(), statuser = TILSAGN_STATUS_VISNING_ARRANGORFLATE)
            if (tilsagn.isEmpty()) {
                call.respond(ArrangorflateTilsagnOversikt())
            } else {
                val tabell = tilsagnOversiktDataDrivenTable(tilsagn)
                call.respond(ArrangorflateTilsagnOversikt(tabell))
            }
        }

        get("/{id}", {
            description = "Hent tilsagn"
            tags = setOf("Arrangorflate")
            operationId = "getArrangorflateTilsagn"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tilsagn for gitt id"
                    body<ArrangorflateTilsagnDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val tilsagn = getTilsagnOrRespondNotFound()

            requireTilgangHosArrangor(altinnRettigheterService, tilsagn.arrangor.organisasjonsnummer)

            call.respond(tilsagn)
        }
    }

    get(
        "/utbetaling",
        {
            description = "Hent oversikt over utbetalinger for alle arrangører brukeren har tilgang til"
            tags = setOf("Arrangorflate")
            operationId = "getArrangorflateUtbetalinger"
            request {
                queryParameter<UtbetalingOversiktType>("type")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Utbetalinger i tabellformat"
                    body<ArrangorflateUtbetalingerOversikt>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        },
    ) {
        val tilganger = orgnrTilganger(altinnRettigheterService)
        if (tilganger.isEmpty()) {
            respondWithManglerTilgangHosArrangor()
            return@get
        }
        val type = UtbetalingOversiktType.from(call.queryParameters["type"])
        val utbetalinger =
            arrangorFlateService.getUtbetalingerByArrangorerAndStatus(tilganger.toSet(), type.utbetalingStatuser())
        if (utbetalinger.isEmpty()) {
            call.respond(ArrangorflateUtbetalingerOversikt())
        } else {
            val tabell = utbetalingKompaktDataDrivenTable(type, utbetalinger)
            call.respond(ArrangorflateUtbetalingerOversikt(tabell))
        }
    }

    route("/utbetaling/{id}") {
        get({
            description = "Hent utbetaling for arrangør"
            tags = setOf("Arrangorflate")
            operationId = "getArrangorflateUtbetaling"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Utbetaling for gitt id"
                    body<ArrangorflateUtbetalingDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(altinnRettigheterService, utbetaling.arrangor.organisasjonsnummer)

            val arrangorFlateUtbetaling = arrangorFlateService.toArrangorflateUtbetaling(utbetaling)
            call.respond(arrangorFlateUtbetaling)
        }

        post("/godkjenn", {
            description = "Godkjenn en utbetaling på vegne av arrangør"
            tags = setOf("Arrangorflate")
            operationId = "godkjennUtbetaling"
            request {
                pathParameterUuid("id")
                body<GodkjennUtbetaling>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Utbetaling ble godkjent"
                }
                code(HttpStatusCode.BadRequest) {
                    description = "Valideringsfeil"
                    body<ValidationError>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(altinnRettigheterService, utbetaling.arrangor.organisasjonsnummer)

            val request = call.receive<GodkjennUtbetaling>()

            val advarsler = arrangorFlateService.getAdvarsler(utbetaling)

            UtbetalingValidator
                .validerGodkjennUtbetaling(
                    request,
                    utbetaling,
                    advarsler,
                    today = LocalDate.now(),
                )
                .onLeft {
                    call.respondWithProblemDetail(ValidationError(errors = it))
                }
                .onRight {
                    utbetalingService.godkjentAvArrangor(utbetaling.id, it)
                    call.respond(HttpStatusCode.OK)
                }
        }

        post("/avbryt", {
            description = "Avbryt en utbetaling på vegne av arrangør"
            tags = setOf("Arrangorflate")
            operationId = "avbrytUtbetaling"
            request {
                pathParameterUuid("id")
                body<AvbrytUtbetaling>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Utbetaling ble avbrutt"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()
            requireTilgangHosArrangor(altinnRettigheterService, utbetaling.arrangor.organisasjonsnummer)
            val request = call.receive<AvbrytUtbetaling>()

            UtbetalingValidator
                .validerAvbrytUtbetaling(request, utbetaling)
                .onLeft {
                    call.respondWithProblemDetail(ValidationError(errors = it))
                }
                .onRight {
                    db.session { queries.utbetaling.avbrytUtbetaling(utbetaling.id, it, Instant.now()) }
                    call.respond(HttpStatusCode.OK)
                }
        }

        post("/regenerer", {
            description = "Regenererer en utbetaling på vegne av arrangør"
            tags = setOf("Arrangorflate")
            operationId = "regenererUtbetaling"
            request {
                pathParameterUuid("id")
                body<AvbrytUtbetaling>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Utbetaling ble regenerert"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()
            requireTilgangHosArrangor(altinnRettigheterService, utbetaling.arrangor.organisasjonsnummer)

            UtbetalingValidator
                .validerRegenererUtbetaling(utbetaling)
                .onLeft {
                    call.respondWithProblemDetail(ValidationError(errors = it))
                }
                .onRight {
                    genererUtbetalingService.regenererUtbetaling(utbetaling)
                    call.respond(HttpStatusCode.OK)
                }
        }

        get("/pdf", {
            description = "Hent pdf med status på utbetalingen"
            tags = setOf("Arrangorflate")
            operationId = "getUtbetalingPdf"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Informasjon om utbetalingen"
                    body<String> {
                        mediaTypes(ContentType.Application.Pdf)
                    }
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(altinnRettigheterService, utbetaling.arrangor.organisasjonsnummer)

            val linjer = arrangorFlateService.getLinjer(utbetaling.id)
            val content = UbetalingToPdfDocumentContentMapper.toUtbetalingsdetaljerPdfContent(utbetaling, linjer)
            pdfClient.getPdfDocument(content)
                .onRight { pdfContent ->
                    call.response.headers.append(
                        "Content-Disposition",
                        "attachment; filename=\"utbetaling.pdf\"",
                    )
                    call.respondBytes(pdfContent, contentType = ContentType.Application.Pdf)
                }
                .onLeft { error ->
                    application.log.warn("Klarte ikke lage PDF. Response fra pdfgen: $error")
                    call.respondWithProblemDetail(InternalServerError("Klarte ikke lage PDF"))
                }
        }

        get("/tilsagn", {
            description = "Hent alle tilsagn som kan benyttes til utbetalingen"
            tags = setOf("Arrangorflate")
            operationId = "getArrangorflateTilsagnTilUtbetaling"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle tilsagn som kan benyttes til utbetalingen"
                    body<List<ArrangorflateTilsagnDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(altinnRettigheterService, utbetaling.arrangor.organisasjonsnummer)

            val tilsagn = arrangorFlateService.getArrangorflateTilsagnTilUtbetaling(utbetaling)

            call.respond(tilsagn)
        }

        get("/sync-kontonummer", {
            description =
                "Oppdaterer utbetalingen med nyeste kontonummer registrert for arrangør sitt organisasjonsnummer"
            tags = setOf("Arrangorflate")
            operationId = "synkroniserKontonummerForUtbetaling"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Kontonummer som blir benyttet for utbetalingen"
                    body<KontonummerResponse>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(altinnRettigheterService, utbetaling.arrangor.organisasjonsnummer)

            arrangorFlateService.synkroniserKontonummer(utbetaling)
                .onLeft { error ->
                    call.respondWithProblemDetail(
                        when (error) {
                            KontonummerRegisterOrganisasjonError.UgyldigInput -> BadRequest("Ugyldig input")
                            KontonummerRegisterOrganisasjonError.FantIkkeKontonummer -> NotFound("Organisasjon mangler kontonummer")
                            KontonummerRegisterOrganisasjonError.Error -> InternalServerError("Klarte ikke hente kontonummer for organisasjon")
                        },
                    )
                }
                .onRight { kontonummer ->
                    call.respond(KontonummerResponse(kontonummer))
                }
        }
    }

    route("/vedlegg") {
        post("/scan", {
            description = "Antivirus scan av vedlegg"
            tags = setOf("Arrangorflate")
            operationId = "scanVedlegg"
            request {
                body<ScanVedleggRequest> {
                    description = "Vedleggene som skal scannes"
                    mediaTypes(ContentType.MultiPart.FormData)
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Fant ikke virus i vedleggene"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = receiveScanVedleggRequest(call)

            if (clamAvClient.virusScanVedlegg(request.vedlegg).any { it.Result == Status.FOUND }) {
                return@post call.respondWithProblemDetail(BadRequest("Virus funnet i minst ett vedlegg"))
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}

private suspend fun receiveScanVedleggRequest(call: RoutingCall): ScanVedleggRequest {
    val vedlegg: MutableList<Vedlegg> = mutableListOf()
    val multipart = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 100)

    multipart.forEachPart { part ->
        when (part) {
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

    val validatedVedlegg = vedlegg.validateVedlegg()

    return ScanVedleggRequest(validatedVedlegg)
}

fun MutableList<Vedlegg>.validateVedlegg(): List<Vedlegg> {
    return this.map { v ->
        // Optionally validate file type and size here
        val fileName = v.filename
        val contentType = v.content.contentType

        require(contentType.equals("application/pdf", ignoreCase = true)) {
            "Vedlegg $fileName er ikke en PDF"
        }

        v
    }
}

@Serializable
data class KontonummerResponse(
    val kontonummer: Kontonummer,
)

@Serializable
data class GodkjennUtbetaling(
    val digest: String,
    val kid: String?,
)

@Serializable
data class AvbrytUtbetaling(
    val begrunnelse: String?,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class DeltakerAdvarsel {
    abstract val deltakerId: UUID
    abstract val beskrivelse: String

    @Serializable
    @SerialName("DeltakerAdvarselRelevanteForslag")
    data class RelevanteForslag(
        @Serializable(with = UUIDSerializer::class)
        override val deltakerId: UUID,
        override val beskrivelse: String,
    ) : DeltakerAdvarsel()

    @Serializable
    @SerialName("DeltakerAdvarselFeilSluttDato")
    data class FeilSluttDato(
        @Serializable(with = UUIDSerializer::class)
        override val deltakerId: UUID,
        override val beskrivelse: String,
    ) : DeltakerAdvarsel()
}

@Serializable
data class ScanVedleggRequest(
    val vedlegg: List<Vedlegg>,
)

@Serializable
data class OpprettKravOmUtbetalingResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)

@Serializable
enum class UtbetalingOversiktType {
    AKTIVE,
    HISTORISKE,
    ;

    fun utbetalingStatuser(): Set<UtbetalingStatusType> = when (this) {
        AKTIVE -> setOf(
            UtbetalingStatusType.GENERERT,
            UtbetalingStatusType.INNSENDT,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
        )

        HISTORISKE -> setOf(
            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            UtbetalingStatusType.AVBRUTT,
        )
    }

    companion object {
        /**
         * Defaulter til AKTIVE
         */
        fun from(type: String?): UtbetalingOversiktType = when (type) {
            "AKTIVE" -> AKTIVE
            "HISTORISKE" -> HISTORISKE
            else -> AKTIVE
        }
    }
}

@Serializable
data class ArrangorflateTilsagnOversikt(val tabell: DataDrivenTableDto? = null)

val TILSAGN_STATUS_VISNING_ARRANGORFLATE = listOf(
    TilsagnStatus.GODKJENT,
    TilsagnStatus.TIL_ANNULLERING,
    TilsagnStatus.ANNULLERT,
    TilsagnStatus.TIL_OPPGJOR,
    TilsagnStatus.OPPGJORT,
)

fun tilsagnOversiktDataDrivenTable(
    tilsagnListe: List<ArrangorflateTilsagnDto>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = listOf(
            DataDrivenTableDto.Column("tiltak", "Tiltak"),
            DataDrivenTableDto.Column("arrangor", "Arrangør"),
            DataDrivenTableDto.Column("periode", "Periode"),
            DataDrivenTableDto.Column(
                "tilsagn",
                "Tilsagn",
            ),
            DataDrivenTableDto.Column("status", "Status"),
            DataDrivenTableDto.Column(
                "action",
                "Handlinger",
                sortable = false,
            ),
        ),
        rows = tilsagnListe.map { tilsagn ->
            DataDrivenTableDto.Row(
                cells = mapOf(
                    "tiltak" to DataElement.text("${tilsagn.tiltakstype.navn} (${tilsagn.gjennomforing.lopenummer.value})"),
                    "arrangor" to DataElement.text(
                        "${tilsagn.arrangor.navn} (${tilsagn.arrangor.organisasjonsnummer.value})",
                    ),
                    "periode" to DataElement.periode(tilsagn.periode),
                    "tilsagn" to DataElement.text("${tilsagn.type.displayName()} (${tilsagn.bestillingsnummer})"),
                    "status" to getTilsagnStatus(tilsagn.status),
                    "action" to DataElement.Link(
                        "Se detaljer",
                        "/${tilsagn.arrangor.organisasjonsnummer}/tilsagn/${tilsagn.id}",
                    ),
                ),
            )
        },
    )
}

fun getTilsagnStatus(tilsagnStatus: TilsagnStatus): DataElement = when (tilsagnStatus) {
    TilsagnStatus.RETURNERT,
    TilsagnStatus.TIL_GODKJENNING,
    ->
        throw IllegalStateException("Skal ikke vise tilsagn som ikke har vært godkjent")

    TilsagnStatus.GODKJENT ->
        DataElement.Status("Godkjent", DataElement.Status.Variant.SUCCESS)

    TilsagnStatus.TIL_ANNULLERING ->
        DataElement.Status("Til annullering", DataElement.Status.Variant.WARNING)

    TilsagnStatus.ANNULLERT ->
        DataElement.Status("Annulert", DataElement.Status.Variant.ERROR_BORDER_STRIKETHROUGH)

    TilsagnStatus.TIL_OPPGJOR ->
        DataElement.Status("Til oppgjør", DataElement.Status.Variant.WARNING)

    TilsagnStatus.OPPGJORT ->
        DataElement.Status("Oppgjort", DataElement.Status.Variant.NEUTRAL)
}

@Serializable
data class ArrangorflateUtbetalingerOversikt(val tabell: DataDrivenTableDto? = null)

fun utbetalingKompaktDataDrivenTable(
    tabellType: UtbetalingOversiktType,
    utbetalinger: List<ArrangorflateUtbetalingKompaktDto>,
): DataDrivenTableDto {
    return DataDrivenTableDto(
        columns = listOf(
            DataDrivenTableDto.Column("tiltak", "Tiltak"),
            DataDrivenTableDto.Column("arrangor", "Arrangør"),
            DataDrivenTableDto.Column("periode", "Periode"),
            DataDrivenTableDto.Column(
                "belop",
                when (tabellType) {
                    UtbetalingOversiktType.AKTIVE -> "Beløp"
                    UtbetalingOversiktType.HISTORISKE -> "Godkjent beløp"
                },
                sortable = true,
                align = DataDrivenTableDto.Column.Align.RIGHT,
            ),
            DataDrivenTableDto.Column("type", "Type"),
            DataDrivenTableDto.Column("status", "Status"),
            DataDrivenTableDto.Column(
                "action",
                "Handlinger",
                sortable = false,
            ),
        ),
        rows = utbetalinger.map { utbetaling ->
            DataDrivenTableDto.Row(
                cells = mapOf(
                    "tiltak" to DataElement.text("${utbetaling.tiltakstype.navn} (${utbetaling.gjennomforing.lopenummer.value})"),
                    "arrangor" to DataElement.text(
                        "${utbetaling.arrangor.navn} (${utbetaling.arrangor.organisasjonsnummer.value})",
                    ),
                    "periode" to DataElement.periode(utbetaling.periode),
                    "belop" to DataElement.nok(
                        when (tabellType) {
                            UtbetalingOversiktType.AKTIVE -> utbetaling.belop
                            UtbetalingOversiktType.HISTORISKE -> utbetaling.godkjentBelop
                        },
                    ),
                    "type" to getUtbetalingType(utbetaling),
                    "status" to getUtbetalingStatus(utbetaling.status),
                    "action" to getUtbetalingLinkByStatus(utbetaling),
                ),
            )
        },
    )
}

private fun getUtbetalingType(utbetaling: ArrangorflateUtbetalingKompaktDto): DataElement? {
    return utbetaling.type.tagName?.let {
        DataElement.Status(it, DataElement.Status.Variant.NEUTRAL)
    }
}

private fun getUtbetalingStatus(status: ArrangorflateUtbetalingStatus): DataElement = when (status) {
    KLAR_FOR_GODKJENNING -> DataElement.Status(
        "Klar for innsending",
        variant = DataElement.Status.Variant.ALT_1,
    )

    KREVER_ENDRING -> DataElement.Status(
        "Krever endring",
        variant = DataElement.Status.Variant.WARNING,
    )

    BEHANDLES_AV_NAV -> DataElement.Status(
        "Behandles av Nav",
        variant = DataElement.Status.Variant.WARNING,
    )

    OVERFORT_TIL_UTBETALING -> DataElement.Status(
        "Overført til utbetaling",
        variant = DataElement.Status.Variant.SUCCESS,
    )

    DELVIS_UTBETALT -> DataElement.Status(
        "Delvis utbetalt",
        variant = DataElement.Status.Variant.SUCCESS,
    )

    UTBETALT -> DataElement.Status(
        "Utbetalt",
        variant = DataElement.Status.Variant.SUCCESS,
    )

    AVBRUTT -> DataElement.Status(
        "Avbrutt av arrangør",
        variant = DataElement.Status.Variant.ERROR,
    )
}

private fun getUtbetalingLinkByStatus(utbetaling: ArrangorflateUtbetalingKompaktDto): DataElement = when (utbetaling.status) {
    KLAR_FOR_GODKJENNING ->
        DataElement.Link(
            text = "Start innsending",
            href = "${utbetaling.arrangor.organisasjonsnummer}/utbetaling/${utbetaling.id}/innsendingsinformasjon",
        )

    KREVER_ENDRING ->
        DataElement.Link(
            text = "Se innsending",
            href = "${utbetaling.arrangor.organisasjonsnummer}/utbetaling/${utbetaling.id}/beregning",
        )

    BEHANDLES_AV_NAV,
    OVERFORT_TIL_UTBETALING,
    DELVIS_UTBETALT,
    UTBETALT,
    AVBRUTT,
    ->
        DataElement.Link(
            text = "Se detaljer",
            href = "${utbetaling.arrangor.organisasjonsnummer}/utbetaling/${utbetaling.id}/detaljer",
        )
}
