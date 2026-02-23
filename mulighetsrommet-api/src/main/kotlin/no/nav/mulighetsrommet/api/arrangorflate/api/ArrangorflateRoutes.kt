package no.nav.mulighetsrommet.api.arrangorflate.api

import arrow.core.getOrElse
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.application
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.altinn.AltinnError
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorInnsendingRadDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.toRadDto
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflateService
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
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.clamav.ClamAvClient
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
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
    val utbetalingService: UtbetalingService by inject()
    val pdfClient: PdfGenClient by inject()
    val arrangorFlateService: ArrangorflateService by inject()
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
                operationId = "getArrangorflateTilsagnRader"
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilsagn i tabellrad format"
                        body<List<ArrangorflateTilsagnRadDto>>()
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
            call.respond(tilsagn.map { it.toRadDto() })
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
                    body<List<ArrangorInnsendingRadDto>>()
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

        call.respond(utbetalinger.map { it.toRadDto() })
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

        get("/kvittering", {
            description = "Hent utbetalingskvittering for arrangør"
            tags = setOf("Arrangorflate")
            operationId = "getUtbetalingKvittering"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Kvitteringsdata for gitt id"
                    body<ArrangorflateUtbetalingKvittering>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val utbetaling = getUtbetalingOrRespondNotFound()

            requireTilgangHosArrangor(altinnRettigheterService, utbetaling.arrangor.organisasjonsnummer)

            if (utbetaling.godkjentAvArrangorTidspunkt != null) {
                call.respond(
                    ArrangorflateUtbetalingKvittering(
                        id = utbetaling.id,
                        mottattDato = utbetaling.godkjentAvArrangorTidspunkt.toLocalDate(),
                        utbetalesTidligstDato = utbetaling.utbetalesTidligstTidspunkt?.tilNorskDato(),
                        kontonummer = when (utbetaling.betalingsinformasjon) {
                            is Betalingsinformasjon.BBan -> utbetaling.betalingsinformasjon.kontonummer
                            is Betalingsinformasjon.IBan -> null
                            null -> null
                        },
                    ),
                )
            } else {
                call.respondWithProblemDetail(NotFound("Utbetalingskravet er ikke sendt inn. Ingen kvittering tilgjengelig."))
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
                    utbetalingService.avbrytUtbetaling(utbetaling.id, it)
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
}

@Serializable
data class KontonummerResponse(
    val kontonummer: Kontonummer,
)

@Serializable
data class GodkjennUtbetaling(
    val updatedAt: String,
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

val TILSAGN_STATUS_VISNING_ARRANGORFLATE = listOf(
    TilsagnStatus.GODKJENT,
    TilsagnStatus.TIL_ANNULLERING,
    TilsagnStatus.ANNULLERT,
    TilsagnStatus.TIL_OPPGJOR,
    TilsagnStatus.OPPGJORT,
)

@Serializable
data class ArrangorflateTilsagnRadDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val organisasjonsnummer: Organisasjonsnummer,
    val tiltakTypeNavn: String,
    val tiltakNavn: String,
    val arrangorNavn: String,
    val periode: Periode,
    val tilsagnNavn: String,
    val status: TilsagnStatus,
)

fun ArrangorflateTilsagnDto.toRadDto(): ArrangorflateTilsagnRadDto = ArrangorflateTilsagnRadDto(
    id = id,
    organisasjonsnummer = arrangor.organisasjonsnummer,
    tiltakTypeNavn = tiltakstype.navn,
    tiltakNavn = "${gjennomforing.navn} (${gjennomforing.lopenummer})",
    arrangorNavn = "${arrangor.navn} (${arrangor.organisasjonsnummer.value})",
    periode = periode,
    tilsagnNavn = "${type.displayName()} ($bestillingsnummer)",
    status = status,
)

@Serializable
data class ArrangorflateUtbetalingKvittering(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val mottattDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val utbetalesTidligstDato: LocalDate?,
    val kontonummer: Kontonummer?,
)
