package no.nav.mulighetsrommet.api.arrangorflate.api

import arrow.core.flatMap
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
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTilsagnKompakt
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflateService
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflateUtbetalingService
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.plugins.ArrangorflatePrincipal
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.mapper.UbetalingToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.database.utils.map
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

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
): Set<Organisasjonsnummer> {
    return call.principal<ArrangorflatePrincipal>()?.norskIdent?.let {
        altinnRettigheterService.getRettigheter(it)
            .getOrElse { err ->
                when (err) {
                    AltinnError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke få kontakt med Altinn. Vennligst prøv igjen senere",
                    )
                }
            }
            .filter { AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING in it.rettigheter }
            .map { bedriftRettighet -> bedriftRettighet.organisasjonsnummer }
            .toSet()
    } ?: throw StatusException(HttpStatusCode.InternalServerError, "Principal var null. Dette skal ikke kunne skje")
}

suspend fun RoutingContext.requireTilgangHosArrangor(
    altinnRettigheterService: AltinnRettigheterService,
    organisasjonsnummer: Organisasjonsnummer,
): Organisasjonsnummer {
    return orgnrTilganger(altinnRettigheterService).find { it == organisasjonsnummer }
        ?: throw StatusException(
            HttpStatusCode.Forbidden,
            "Ikke tilgang til bedrift med organisasjonsnummer $organisasjonsnummer",
        )
}

fun Route.arrangorflateRoutes(config: AppConfig) {
    val db: ApiDatabase by inject()
    val utbetalingService: ArrangorflateUtbetalingService by inject()
    val pdfClient: PdfGenClient by inject()
    val arrangorflateService: ArrangorflateService by inject()
    val altinnRettigheterService: AltinnRettigheterService by inject()

    suspend fun RoutingContext.getTilsagnOrRespondWithClientError(): ArrangorflateTilsagnDto {
        val id: UUID by call.parameters

        val tilsagn = arrangorflateService.getTilsagn(id)
            ?: throw NotFoundException("Fant ikke tilsagn med id=$id")

        requireTilgangHosArrangor(altinnRettigheterService, tilsagn.arrangor.organisasjonsnummer)

        return tilsagn
    }

    suspend fun RoutingContext.getUtbetalingOrRespondWithClientError(): Utbetaling {
        val id: UUID by call.parameters

        val utbetaling = utbetalingService.getUtbetaling(id)
            ?: throw NotFoundException("Fant ikke utbetaling med id=$id")

        requireTilgangHosArrangor(altinnRettigheterService, utbetaling.arrangor.organisasjonsnummer)

        return utbetaling
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
                queryParameter<String>("search")
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
                return@get respondWithManglerTilgangHosArrangor()
            }
            val filter = getArrangorflateTilsagnFilter()
            val (totalCount, data) = db.session {
                queries.arrangorflate.tilsagn
                    .getFiltered(
                        arrangorer = tilganger,
                        filter = filter,
                    )
                    .map { it.toRadDto() }
            }
            val response = PaginatedResponse.of(filter.pagination, totalCount, data)
            call.respond(response)
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
            val tilsagn = getTilsagnOrRespondWithClientError()
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
            return@get respondWithManglerTilgangHosArrangor()
        }
        val filter = getArrangorflateUtbetalingFilter(tilganger)
        val (totalCount, items) = arrangorflateService.getAllUtbetalingKompakt(filter)
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
            val utbetaling = getUtbetalingOrRespondWithClientError()

            val response = arrangorflateService.toArrangorflateUtbetaling(utbetaling)

            call.respond(response)
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
            val utbetaling = getUtbetalingOrRespondWithClientError()

            val request = call.receive<GodkjennUtbetaling>()

            request.validate(utbetaling)
                .flatMap { kid ->
                    utbetalingService.godkjentAvArrangor(utbetaling.id, kid)
                }
                .onLeft { errors ->
                    call.respondWithProblemDetail(ValidationError("Utbetalingen kan ikke godkjennes", errors))
                }
                .onRight {
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
            val utbetaling = getUtbetalingOrRespondWithClientError()

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
            val utbetaling = getUtbetalingOrRespondWithClientError()

            val request = call.receive<AvbrytUtbetaling>()

            request.validate()
                .flatMap { begrunnelse ->
                    utbetalingService.avbrytUtbetaling(utbetaling.id, begrunnelse)
                }
                .onLeft { errors ->
                    call.respondWithProblemDetail(ValidationError("Utbetalingen kan ikke avbrytes", errors))
                }
                .onRight {
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
            val utbetaling = getUtbetalingOrRespondWithClientError()

            utbetalingService.regenererUtbetaling(utbetaling)
                .onLeft {
                    call.respondWithProblemDetail(ValidationError("Utbetalingen kan ikke regenereres", it))
                }
                .onRight {
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
            val utbetaling = getUtbetalingOrRespondWithClientError()
            val linjer = arrangorflateService.getLinjer(utbetaling.id)
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
            val utbetaling = getUtbetalingOrRespondWithClientError()

            val tilsagn = arrangorflateService.getArrangorflateTilsagnTilUtbetaling(utbetaling)

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
            val utbetaling = getUtbetalingOrRespondWithClientError()

            arrangorflateService.synkroniserKontonummer(utbetaling)
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
) {
    @OptIn(ExperimentalContracts::class)
    fun validate(utbetaling: Utbetaling): Validated<Kid?> = validation {
        validate(updatedAt == utbetaling.updatedAt.toString()) {
            FieldError.of("Informasjonen i kravet har endret seg. Vennligst se over på nytt.")
        }
        requireValid(kid == null || Kid.parse(kid) != null) {
            FieldError.of("Ugyldig kid", GodkjennUtbetaling::kid)
        }
        kid?.let { Kid.parseOrThrow(it) }
    }
}

@Serializable
data class AvbrytUtbetaling(
    val begrunnelse: String?,
) {
    @OptIn(ExperimentalContracts::class)
    fun validate(): Validated<String> = validation {
        requireValid(!begrunnelse.isNullOrBlank()) {
            FieldError.of("Begrunnelse må være satt", AvbrytUtbetaling::begrunnelse)
        }
        validate(begrunnelse.length <= 100) {
            FieldError.of("Begrunnelse kan ikke være lengre enn 100 tegn", AvbrytUtbetaling::begrunnelse)
        }
        begrunnelse
    }
}

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
    val tilsagnType: String,
    val bestillingsnummer: String,
    val status: TilsagnStatus,
)

private fun ArrangorflateTilsagnKompakt.toRadDto(): ArrangorflateTilsagnRadDto = ArrangorflateTilsagnRadDto(
    id = id,
    organisasjonsnummer = arrangor.organisasjonsnummer,
    tiltakTypeNavn = tiltakstype.navn,
    tiltakNavn = "${gjennomforing.navn} (${gjennomforing.lopenummer})",
    arrangorNavn = "${arrangor.navn} (${arrangor.organisasjonsnummer.value})",
    periode = periode,
    tilsagnType = type.displayName(),
    bestillingsnummer = bestillingsnummer,
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
