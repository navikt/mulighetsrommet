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
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.altinn.AltinnError
import no.nav.mulighetsrommet.altinn.AltinnRettigheterService
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorInnsendingRadDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterType
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnFilter
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingFilter
import no.nav.mulighetsrommet.api.arrangorflate.dto.getArrangorflateTilsagnFilter
import no.nav.mulighetsrommet.api.arrangorflate.dto.getArrangorflateUtbetalingFilter
import no.nav.mulighetsrommet.api.arrangorflate.dto.toRadDto
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflateService
import no.nav.mulighetsrommet.api.arrangorflate.service.TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.mapper.UbetalingToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingValidator
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.Arrangor
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

suspend fun RoutingContext.orgnrTilganger(
    altinnRettigheterService: AltinnRettigheterService,
): List<Organisasjonsnummer> {
    return call.principal<ArrangorflatePrincipal>()?.norskIdent?.let {
        altinnRettigheterService.getRettigheter(it)
            .getOrElse {
                when (it) {
                    AltinnError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke få kontakt med Altinn. Vennligst prøv igjen senere",
                    )
                }
            }
            .filter { AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING in it.rettigheter }
            .map { it.organisasjonsnummer }
    } ?: throw StatusException(HttpStatusCode.InternalServerError, "Principal var null. Dette skal ikke kunne skje")
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
    val altinnRettigheterService: AltinnRettigheterService by inject()
    val genererUtbetalingService: GenererUtbetalingService by inject()

    suspend fun RoutingContext.getTilsagnOrRespondNotFound(): ArrangorflateTilsagnDto {
        val id: UUID by call.parameters
        return arrangorFlateService.getTilsagn(id) ?: throw NotFoundException("Fant ikke tilsagn med id=$id")
    }

    fun RoutingContext.getUtbetalingOrRespondNotFound(): Utbetaling {
        val id: UUID by call.parameters
        return arrangorFlateService.getUtbetaling(id) ?: throw NotFoundException("Fant ikke utbetaling med id=$id")
    }

    arrangorflateOpprettKravRoutes(config.okonomi)

    route("/orgnr-tilganger") {
        get(
            {
                description = "Hent ut listen over orgnr bruker har tilgang til"
                tags = setOf("Arrangorflate")
                operationId = "getOrganisasjonTilganger"
                response {
                    code(HttpStatusCode.OK) {
                        description = "Organisasjonsnumre som brukeren har tilgang til"
                        body<List<Organisasjonsnummer>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            },
        ) {
            val organisasjonsTilganger = orgnrTilganger(altinnRettigheterService)
            call.respond(organisasjonsTilganger)
        }
    }
    route("/tilsagn") {
        get({
            description = "Hent oversikt over tilsagn for alle arrangører brukeren har tilgang til"
            tags = setOf("Arrangorflate")
            operationId = "getArrangorflateTilsagnRader"
            request {
                queryParameter<Int>("page")
                queryParameter<Int>("size")
                queryParameter<ArrangorflateTilsagnFilter.OrderBy>("orderBy")
                queryParameter<ArrangorflateFilterDirection>("direction")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Paginert tilsagn i tabellrad format"
                    body<PaginatedResponse<ArrangorflateTilsagnRadDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val tilganger = orgnrTilganger(altinnRettigheterService)
            if (tilganger.isEmpty()) {
                respondWithManglerTilgangHosArrangor()
                return@get
            }
            val filter = getArrangorflateTilsagnFilter()
            val (totalCount, data) = db.session {
                queries.tilsagn
                    .getArrangorflateFiltered(
                        arrangorer = tilganger,
                        filter = filter,
                        statuser = TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR,
                    )
            }
            call.respond(
                PaginatedResponse.of(
                    filter.pagination,
                    totalCount,
                    data
                        .map { ArrangorflateTilsagnDto.from(it, arrangorFlateService.getTilsagnDeltakerPersonalia(it.deltakere)) }
                        .map { it.toRadDto() },
                ),
            )
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

    get("/utbetaling", {
        description = "Hent oversikt over utbetalinger for alle arrangører brukeren har tilgang til"
        tags = setOf("Arrangorflate")
        operationId = "getArrangorflateUtbetalinger"
        request {
            queryParameter<String>("sok")
            queryParameter<Int>("page")
            queryParameter<Int>("size")
            queryParameter<ArrangorflateFilterType>("type")
            queryParameter<ArrangorflateUtbetalingFilter.OrderBy>("orderBy")
            queryParameter<ArrangorflateFilterDirection>("direction")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Utbetalinger i tabellformat"
                body<PaginatedResponse<ArrangorInnsendingRadDto>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val tilganger = orgnrTilganger(altinnRettigheterService)
        if (tilganger.isEmpty()) {
            respondWithManglerTilgangHosArrangor()
            return@get
        }

        val filter = getArrangorflateUtbetalingFilter(tilganger.toSet())
        val (totalCount, items) = arrangorFlateService.getAllUtbetalingKompakt(filter)
        val response = PaginatedResponse.of(filter.pagination, totalCount, items.map { it.toRadDto() })

        call.respond(response)
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

            if (utbetaling.innsending == null) {
                return@get call.respondWithProblemDetail(NotFound("Utbetalingskravet er ikke sendt inn. Ingen kvittering tilgjengelig."))
            }

            val kvittering = ArrangorflateUtbetalingKvittering(
                id = utbetaling.id,
                mottattDato = utbetaling.innsending.tidspunkt.toLocalDate(),
                utbetalesTidligstDato = utbetaling.utbetalesTidligstTidspunkt?.tilNorskDato(),
                kontonummer = when (utbetaling.betalingsinformasjon) {
                    is Betalingsinformasjon.BBan -> utbetaling.betalingsinformasjon.kontonummer
                    is Betalingsinformasjon.IBan -> null
                    null -> null
                },
            )

            call.respond(kvittering)
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
                .map { utbetalingService.avbrytUtbetaling(utbetaling.id, it, Arrangor) }
                .onRight {
                    call.respond(HttpStatusCode.OK)
                }
                .onLeft {
                    call.respondWithProblemDetail(ValidationError(errors = it))
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
            val gjennomforing = db.session {
                queries.gjennomforing.getGjennomforingAvtaleOrError(utbetaling.gjennomforing.id)
            }
            val content = UbetalingToPdfDocumentContentMapper.toUtbetalingsdetaljerPdfContent(
                utbetaling,
                linjer,
                gjennomforing,
            )
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

@Serializable
data class OpprettKravOmUtbetalingResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
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
