package no.nav.mulighetsrommet.api.utbetaling.api

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.toNonEmptyListOrNull
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getAccessType
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingDtoQuery
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingDtoQueryError
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinjer
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusAarsak
import no.nav.mulighetsrommet.api.utbetaling.service.AdminUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingError
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingValidator
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.requireAzureAd
import no.nav.mulighetsrommet.validation.validation
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

fun Route.utbetalingRoutes() {
    val db: ApiDatabase by inject()
    val utbetalingService: AdminUtbetalingService by inject()
    val utbetalingDtoQuery: UtbetalingDtoQuery by inject()

    get("/utbetaling", {
        description = "Hent alle utbetalinger for gitt gjennomføring"
        tags = setOf("Utbetaling")
        operationId = "getUtbetalinger"
        request {
            queryParameterUuid("gjennomforingId") {
                required = true
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Alle utbetalinger for gitt gjennomføring"
                body<List<UtbetalingKompaktDto>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val gjennomforingId: UUID by call.queryParameters

        val utbetalinger = db.session {
            queries.utbetaling.getByGjennomforing(gjennomforingId).map { utbetaling ->
                val utbetalingLinjer = queries.utbetalingLinje.getByUtbetalingId(utbetaling.id)

                val belopUtbetalt = if (utbetaling.erFerdigBehandlet()) {
                    utbetalingLinjer.sumOf { it.pris.belop }.withValuta(utbetaling.valuta)
                } else {
                    null
                }

                val kostnadssteder = utbetalingLinjer
                    .map { queries.tilsagn.getOrError(it.tilsagnId).kostnadssted }
                    .distinct()

                UtbetalingKompaktDto(
                    id = utbetaling.id,
                    status = UtbetalingStatusDto.fromUtbetalingStatus(
                        utbetaling.status,
                        utbetaling.blokkeringer,
                        utbetaling.avbrytelse?.totrinnskontroll,
                    ),
                    periode = utbetaling.periode,
                    kostnadssteder = kostnadssteder.map { KostnadsstedDto.fromNavEnhet(it) },
                    belopUtbetalt = belopUtbetalt,
                    type = UtbetalingType.from(utbetaling).toDto(),
                )
            }
        }

        call.respond(utbetalinger)
    }

    route("/utbetaling") {
        authorize(anyOf = setOf(Rolle.SAKSBEHANDLER_OKONOMI, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)) {
            post("/opprett", {
                tags = setOf("Utbetaling")
                operationId = "opprettUtbetaling"
                request {
                    body<UtbetalingRequest>()
                }
                response {
                    code(HttpStatusCode.Created) {
                        description = "Utbetalingen ble opprettet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val request = call.receive<UtbetalingRequest>()
                val navIdent = getNavIdent()

                val result = UtbetalingValidator.validateUpsertUtbetaling(request)
                    .mapLeft(UtbetalingError::Valideringsfeil)
                    .flatMap { utbetalingService.opprettUtbetaling(it, navIdent) }
                    .mapLeft { toProblemDetail(it, "Klarte ikke opprette utbetaling") }
                    .map { HttpStatusCode.Created }

                call.respondWithStatusResponse(result)
            }

            post("/rediger", {
                tags = setOf("Utbetaling")
                operationId = "redigerUtbetaling"
                request {
                    body<UtbetalingRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Utbetalingen ble redigert"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val request = call.receive<UtbetalingRequest>()
                val navIdent = getNavIdent()

                val result = UtbetalingValidator.validateUpsertUtbetaling(request)
                    .mapLeft { UtbetalingError.Valideringsfeil(it) }
                    .flatMap { utbetalingService.redigerUtbetaling(it, navIdent) }
                    .mapLeft { toProblemDetail(it, "Klarte ikke redigere utbetaling") }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }
    }

    get("/innsendinger", {
        description = "Hent filtrerte innsendinger"
        tags = setOf("Utbetaling")
        operationId = "getInnsendinger"
        request {
            queryParameter<List<Tiltakskode>>("tiltakstyper") {
                explode = true
            }
            queryParameter<List<NavEnhetNummer>>("navEnheter") {
                explode = true
            }
            queryParameter<String>("sort")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Alle innsendinger for gitte filtre"
                body<List<InnsendingKompaktDto>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val filter = getAdminInnsendingerFilter()

        val innsendinger = db.session {
            queries.utbetaling.getAll(filter.tiltakskoder, filter.navEnheter, filter.sortering)
        }

        call.respond(innsendinger)
    }

    route("/utbetaling/{id}") {
        authorize(
            anyOf = setOf(
                Rolle.OKONOMI_LES,
                Rolle.SAKSBEHANDLER_OKONOMI,
                Rolle.ATTESTANT_UTBETALING,
                Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS,
                Rolle.OKONOMI_BESLUTTER_ENKELTPLASS,
            ),
        ) {
            get({
                description = "Hent detaljer om utbetaling"
                tags = setOf("Utbetaling")
                operationId = "getUtbetaling"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Detaljer om utbetaling"
                        body<UtbetalingDetaljerDto>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()

                val result = utbetalingDtoQuery.getDetaljer(id, navIdent).mapLeft { toProblemDetail(it) }

                call.respondWithStatusResponse(result)
            }

            get("/beregning", {
                tags = setOf("Utbetaling")
                operationId = "getUtbetalingBeregning"
                request {
                    pathParameterUuid("id")
                    queryParameter<List<String>>("navEnheter") {
                        explode = true
                    }
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Utbetalingen ble opprettet"
                        body<UtbetalingBeregningDto>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()
                val filter = getBeregningFilter()
                val onBehalfOf = PersonaliaService.OnBehalfOf.NavAnsatt(call.getAccessType().requireAzureAd())

                val result = utbetalingDtoQuery
                    .getBeregning(id, navIdent, filter, onBehalfOf)
                    .mapLeft { toProblemDetail(it) }

                call.respondWithStatusResponse(result)
            }

            get("/linjer", {
                tags = setOf("Utbetaling")
                operationId = "getUtbetalingsLinjer"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Utbetalingslinjer til utbetaling"
                        body<List<UtbetalingLinjeDto>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()
                val onBehalfOf = PersonaliaService.OnBehalfOf.NavAnsatt(call.getAccessType().requireAzureAd())

                val result = utbetalingDtoQuery
                    .getLinjer(id, navIdent, onBehalfOf)
                    .mapLeft { toProblemDetail(it) }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(anyOf = setOf(Rolle.SAKSBEHANDLER_OKONOMI, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)) {
            delete({
                description = "Slett utbetaling"
                tags = setOf("Utbetaling")
                operationId = "slettUtbetaling"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {}
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()

                val result = utbetalingService.slettUtbetaling(id, navIdent)
                    .mapLeft { toProblemDetail(it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            put("/avbryt", {
                description = "Avbryt utbetaling"
                tags = setOf("Utbetaling")
                operationId = "avbrytUtbetaling"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Utbetaling ble sendt til avbrytelse (totrinnskontroll)"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>>()
                val navIdent = getNavIdent()

                val result = request.validate()
                    .mapLeft { UtbetalingError.Valideringsfeil(it) }
                    .flatMap { utbetalingService.sendTilAvbrytelse(id, navIdent, it) }
                    .mapLeft { toProblemDetail(it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            put("/avbryt/godkjenn", {
                description = "Godkjenn avbrytelse av utbetaling"
                tags = setOf("Utbetaling")
                operationId = "godkjennAvbrytelseUtbetaling"
                request {
                    pathParameterUuid("id")
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
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()

                val result = utbetalingService.godkjennAvbrytelse(id, navIdent)
                    .mapLeft { toProblemDetail(it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            put("/avbryt/avsla", {
                description = "Avslå avbrytelse av utbetaling"
                tags = setOf("Utbetaling")
                operationId = "avslaAvbrytelseUtbetaling"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Avbrytelse av utbetaling ble avslått, returnert til saksbehandling"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<AarsakerOgBegrunnelseRequest<UtbetalingStatusAarsak>>()
                val navIdent = getNavIdent()

                val result = request.validate()
                    .mapLeft { UtbetalingError.Valideringsfeil(it) }
                    .flatMap { utbetalingService.avslaAvbrytelse(id, navIdent, it) }
                    .mapLeft { toProblemDetail(it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }
    }

    route("/utbetalingslinjer") {
        authorize(anyOf = setOf(Rolle.SAKSBEHANDLER_OKONOMI, Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)) {
            put({
                tags = setOf("Utbetaling")
                operationId = "opprettUtbetalingLinjer"
                request {
                    pathParameterUuid("id")
                    body<OpprettUtbetalingLinjerRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilsanget ble sendt til oppgjør"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val request = call.receive<OpprettUtbetalingLinjerRequest>()
                val navIdent = getNavIdent()

                val result = request.validate()
                    .mapLeft { UtbetalingError.Valideringsfeil(it) }
                    .flatMap { utbetalingService.sendTilAttestering(it, navIdent) }
                    .mapLeft { toProblemDetail(it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(anyOf = setOf(Rolle.ATTESTANT_UTBETALING, Rolle.OKONOMI_BESLUTTER_ENKELTPLASS)) {
            post("/{id}/attester", {
                tags = setOf("Utbetaling")
                operationId = "attesterUtbetalingLinje"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "UtbetalingLinje ble attestert"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val navIdent = getNavIdent()

                val result = utbetalingService.godkjennUtbetalingLinje(id, navIdent)
                    .mapLeft { toProblemDetail(it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(
            anyOf = setOf(
                Rolle.SAKSBEHANDLER_OKONOMI,
                Rolle.ATTESTANT_UTBETALING,
                Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS,
                Rolle.OKONOMI_BESLUTTER_ENKELTPLASS,
            ),
        ) {
            post("/{id}/returner", {
                tags = setOf("Utbetaling")
                operationId = "returnerUtbetalingLinje"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgBegrunnelseRequest<UtbetalingLinjeReturnertAarsak>>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "UtbetalingLinje ble besluttet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val request = call.receive<AarsakerOgBegrunnelseRequest<UtbetalingLinjeReturnertAarsak>>()
                val navIdent = getNavIdent()

                val result = request.validate()
                    .mapLeft { UtbetalingError.Valideringsfeil(it) }
                    .flatMap { utbetalingService.returnerUtbetalingLinje(id, it.aarsaker, it.begrunnelse, navIdent) }
                    .mapLeft { toProblemDetail(it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }
    }
}

data class AdminInnsendingerFilter(
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val tiltakskoder: List<Tiltakskode> = emptyList(),
    val sortering: String? = null,
)

fun RoutingContext.getAdminInnsendingerFilter(): AdminInnsendingerFilter {
    val navEnheter = call.parameters.getAll("navEnheter")?.map { NavEnhetNummer(it) } ?: emptyList()
    val tiltakskoder = call.parameters.getAll("tiltakstyper")
        ?.mapNotNull { runCatching { Tiltakskode.valueOf(it) }.getOrNull() }
        ?: emptyList()
    val sortering = call.request.queryParameters["sort"]

    return AdminInnsendingerFilter(
        navEnheter = navEnheter,
        tiltakskoder = tiltakskoder,
        sortering = sortering,
    )
}

@Serializable
data class UtbetalingLinjeRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    val pris: ValutaBelopRequest? = null,
    val gjorOppTilsagn: Boolean? = false,
)

@Serializable
data class OpprettUtbetalingLinjerRequest(
    @Serializable(with = UUIDSerializer::class)
    val utbetalingId: UUID,
    val utbetalingLinjer: List<UtbetalingLinjeRequest>,
    val begrunnelseMindreBetalt: String?,
) {
    @OptIn(ExperimentalContracts::class)
    fun validate(): Either<List<FieldError>, OpprettUtbetalingLinjer> = validation {
        val linjer = utbetalingLinjer.mapIndexedNotNull { index, req ->
            val belop = req.pris?.belop ?: 0
            if (belop == 0) {
                return@mapIndexedNotNull null
            }

            requireValid(belop > 0 && req.pris?.valuta != null) {
                FieldError("/utbetalingLinjer/$index/pris/belop", "Beløp må være positivt")
            }

            OpprettUtbetalingLinje(
                id = req.id,
                tilsagnId = req.tilsagnId,
                pris = ValutaBelop(belop, requireNotNull(req.pris.valuta)),
                gjorOppTilsagn = req.gjorOppTilsagn ?: false,
            )
        }

        OpprettUtbetalingLinjer(
            utbetalingId = utbetalingId,
            linjer = requireNotNull(linjer.toNonEmptyListOrNull()) {
                FieldError.of("Utbetalingslinjer mangler", OpprettUtbetalingLinjerRequest::utbetalingLinjer)
            },
            begrunnelseMindreBetalt = begrunnelseMindreBetalt?.takeIf { it.isNotBlank() },
        )
    }
}

@Serializable
data class UtbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val korrigererUtbetaling: UUID? = null,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate? = null,
    val journalpostId: String? = null,
    val korreksjonBegrunnelse: String? = null,
    val kommentar: String? = null,
    val kidNummer: String? = null,
    val pris: ValutaBelopRequest? = null,
)

data class BeregningFilter(
    val navEnheter: List<NavEnhetNummer>,
)

fun RoutingContext.getBeregningFilter() = BeregningFilter(
    navEnheter = call.parameters.getAll("navEnheter")?.map { NavEnhetNummer(it) } ?: emptyList(),
)

data class UtbetalingBeregningDeltaker(
    val personalia: Personalia,
    val deltakelse: UtbetalingBeregningOutputDeltakelse,
)

private fun toProblemDetail(error: UtbetalingDtoQueryError): ProblemDetail = when (error) {
    is UtbetalingDtoQueryError.ManglerTilgang -> Forbidden(error.message)
}

private fun toProblemDetail(error: UtbetalingError, title: String? = null): ProblemDetail = when (error) {
    is UtbetalingError.ManglerTilgang -> Forbidden(error.message)

    is UtbetalingError.Valideringsfeil -> if (title == null) {
        ValidationError(errors = error.errors)
    } else {
        ValidationError(title, error.errors)
    }
}
