package no.nav.mulighetsrommet.api.utbetaling.api

import arrow.core.flatMap
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
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.api.plugins.getAccessType
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarselDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.service.AdminUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingValidator
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.requireAzureAd
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.UUID

fun Route.utbetalingRoutes() {
    val db: ApiDatabase by inject()
    val utbetalingService: AdminUtbetalingService by inject()
    val personaliaService: PersonaliaService by inject()

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

                val (belopUtbetalt, kostnadssteder) = if (utbetaling.erFerdigBehandlet()) {
                    Pair(
                        utbetalingLinjer.sumOf { it.pris.belop }.withValuta(utbetaling.valuta),
                        utbetalingLinjer.map { queries.tilsagn.getOrError(it.tilsagnId).kostnadssted }.distinct(),
                    )
                } else {
                    Pair(null, emptyList())
                }

                UtbetalingKompaktDto(
                    id = utbetaling.id,
                    status = UtbetalingStatusDto.fromUtbetalingStatus(utbetaling.status, utbetaling.blokkeringer),
                    periode = utbetaling.periode,
                    kostnadssteder = kostnadssteder.map { KostnadsstedDto.fromNavEnhetDbo(it) },
                    belopUtbetalt = belopUtbetalt,
                    type = UtbetalingType.from(utbetaling).toDto(),
                )
            }
        }

        call.respond(utbetalinger)
    }

    route("/utbetaling") {
        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
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
                    .flatMap { utbetalingService.opprettUtbetaling(it, navIdent) }
                    .mapLeft { ValidationError("Klarte ikke opprette utbetaling", it) }
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
                    .flatMap { utbetalingService.redigerUtbetaling(it, navIdent) }
                    .mapLeft { ValidationError("Klarte ikke redigere utbetaling", it) }
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
        authorize(anyOf = setOf(Rolle.OKONOMI_LES, Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING)) {
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

                val utbetaling = utbetalingService.getUtbetalingDetaljer(id, navIdent)

                call.respond(utbetaling)
            }
        }

        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            delete({
                description = "Slett korreksjon"
                tags = setOf("Utbetaling")
                operationId = "slettKorreksjon"
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

                utbetalingService.slettKorreksjon(id)
                    .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                    .onRight { call.respond(HttpStatusCode.OK) }
            }
        }

        authorize(anyOf = setOf(Rolle.OKONOMI_LES, Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING)) {
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
                val filter = getBeregningFilter()

                val beregning = db.session {
                    val utbetaling = queries.utbetaling.getOrError(id)
                    val deltakelser = utbetaling.beregning.deltakelsePerioder().associateBy { it.deltakelseId }

                    val personalia = personaliaService.getPersonalia(
                        deltakelser.keys.toList(),
                        PersonaliaService.OnBehalfOf.NavAnsatt(call.getAccessType().requireAzureAd()),
                    )

                    val enheter = personalia.flatMap {
                        listOfNotNull(
                            it.oppfolgingEnhet(),
                            it.region(),
                        )
                    }
                    val kontorstruktur = Kontorstruktur.fromNavEnheter(enheter.toList())

                    val deltakelsePersoner = personalia
                        .filter { filter.navEnheter.isEmpty() || it.oppfolgingEnhet()?.enhetsnummer in filter.navEnheter }
                        .associateBy { it.deltakerId }

                    val advarsler = utbetalingService.getAdvarsler(utbetaling)

                    UtbetalingBeregningDto.from(
                        utbetaling.beregning,
                        deltakelsePersoner,
                        kontorstruktur,
                        utbetalingPeriode = utbetaling.periode,
                        advarsler = advarsler.map { advarsel ->
                            DeltakerAdvarselDto.from(advarsel, deltakelsePersoner[advarsel.deltakerId]?.navn())
                        },
                    )
                }

                call.respond(beregning)
            }
        }

        authorize(anyOf = setOf(Rolle.OKONOMI_LES, Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING)) {
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

                val utbetalingsLinjer = utbetalingService.getUtbetalingLinjer(id, navIdent, onBehalfOf)

                call.respond(utbetalingsLinjer)
            }
        }
    }

    route("/utbetalingslinjer") {
        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
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

                val result = utbetalingService.opprettUtbetalingLinjer(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(Rolle.ATTESTANT_UTBETALING) {
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
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }

            post("/{id}/returner", {
                tags = setOf("Utbetaling")
                operationId = "returnerUtbetalingLinje"
                request {
                    pathParameterUuid("id")
                    body<AarsakerOgForklaringRequest<UtbetalingLinjeReturnertAarsak>>()
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
                val request = call.receive<AarsakerOgForklaringRequest<UtbetalingLinjeReturnertAarsak>>()
                val navIdent = getNavIdent()

                val result = request.validate()
                    .flatMap { utbetalingService.returnerUtbetalingLinje(id, it.aarsaker, it.forklaring, navIdent) }
                    .mapLeft { ValidationError(errors = it) }
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
)

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
