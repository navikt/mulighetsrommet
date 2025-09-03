package no.nav.mulighetsrommet.api.utbetaling.api

import arrow.core.flatMap
import arrow.core.right
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.aarsakerforklaring.validateAarsakerOgForklaring
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingReturnertAarsak
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.utbetalingRoutes() {
    val db: ApiDatabase by inject()
    val utbetalingService: UtbetalingService by inject()

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
        val id: UUID by call.queryParameters

        val utbetalinger = utbetalingService.getByGjennomforing(id)

        call.respond(utbetalinger)
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

                val utbetaling = db.session {
                    val ansatt = queries.ansatt.getByNavIdent(navIdent)
                        ?: throw NotFoundException("Fant ikke ansatt med navIdent $navIdent")

                    val utbetaling = queries.utbetaling.get(id)
                        ?: throw NotFoundException("Utbetaling id=$id finnes ikke")

                    val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)
                    val linjer = delutbetalinger.map { delutbetaling ->
                        val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId).let {
                            TilsagnDto.fromTilsagn(it)
                        }

                        val opprettelse = queries.totrinnskontroll
                            .getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
                        val tilsagnOpprettelse = queries.totrinnskontroll
                            .getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)

                        val kanBesluttesAvAnsatt = ansatt.hasKontorspesifikkRolle(
                            Rolle.ATTESTANT_UTBETALING,
                            setOf(tilsagn.kostnadssted.enhetsnummer),
                        ) &&
                            opprettelse.behandletAv != ansatt.navIdent &&
                            tilsagnOpprettelse.besluttetAv != ansatt.navIdent

                        UtbetalingLinje(
                            id = delutbetaling.id,
                            gjorOppTilsagn = delutbetaling.gjorOppTilsagn,
                            belop = delutbetaling.belop,
                            status = DelutbetalingStatusDto.fromDelutbetalingStatus(delutbetaling.status),
                            tilsagn = tilsagn,
                            opprettelse = opprettelse.toDto(kanBesluttesAvAnsatt),
                        )
                    }.sortedBy { it.tilsagn.bestillingsnummer }

                    UtbetalingDetaljerDto(
                        utbetaling = UtbetalingDto.fromUtbetaling(utbetaling),
                        linjer = linjer,
                        handlinger = utbetalingService.handlinger(utbetaling, ansatt),
                    )
                }

                call.respond(utbetaling)
            }
        }

        authorize(anyOf = setOf(Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING)) {
            get("/beregning") {
                val id = call.parameters.getOrFail<UUID>("id")
                val filter = getBeregningFilter()

                val utbetaling = db.session {
                    queries.utbetaling.get(id) ?: throw NotFoundException("Utbetaling id=$id finnes ikke")
                }

                call.respond(utbetalingService.getUtbetalingBeregning(utbetaling, filter = filter))
            }
        }

        get("/historikk") {
            val id = call.parameters.getOrFail<UUID>("id")
            val historikk = db.session {
                queries.endringshistorikk.getEndringshistorikk(DocumentClass.UTBETALING, id)
            }
            call.respond(historikk)
        }

        // TODO: flytt til tilsagnroutes
        get("/tilsagn") {
            val id = call.parameters.getOrFail<UUID>("id")

            val tilsagn = db.session {
                val utbetaling = queries.utbetaling.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

                queries.tilsagn.getAll(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periodeIntersectsWith = utbetaling.periode,
                    typer = TilsagnType.fromTilskuddstype(utbetaling.tilskuddstype),
                ).map { TilsagnDto.fromTilsagn(it) }
            }

            call.respond(tilsagn)
        }

        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            post("/opprett-utbetaling", {
                tags = setOf("Utbetaling")
                operationId = "opprettUtbetaling"
                request {
                    pathParameterUuid("id")
                    body<OpprettUtbetalingRequest>()
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
                val id: UUID by call.parameters
                val request = call.receive<OpprettUtbetalingRequest>()
                val navIdent = getNavIdent()

                val result = UtbetalingValidator.validateOpprettUtbetalingRequest(id, request)
                    .flatMap { utbetalingService.opprettUtbetaling(it, navIdent) }
                    .mapLeft { ValidationError("Klarte ikke opprette utbetaling", it) }
                    .map { HttpStatusCode.Created }

                call.respondWithStatusResponse(result)
            }
        }
    }

    route("/delutbetalinger") {
        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            put({
                tags = setOf("Utbetaling")
                operationId = "opprettDelutbetalinger"
                request {
                    pathParameterUuid("id")
                    body<OpprettDelutbetalingerRequest>()
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
                val request = call.receive<OpprettDelutbetalingerRequest>()
                val navIdent = getNavIdent()

                val result = utbetalingService.opprettDelutbetalinger(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(Rolle.ATTESTANT_UTBETALING) {
            post("/{id}/beslutt", {
                tags = setOf("Utbetaling")
                operationId = "besluttDelutbetaling"
                request {
                    pathParameterUuid("id")
                    body<BesluttTotrinnskontrollRequest<DelutbetalingReturnertAarsak>>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Delutbetaling ble besluttet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val id: UUID by call.parameters
                val request = call.receive<BesluttTotrinnskontrollRequest<DelutbetalingReturnertAarsak>>()
                val navIdent = getNavIdent()

                val result = when (request.besluttelse) {
                    Besluttelse.GODKJENT -> Unit.right()
                    Besluttelse.AVVIST -> validateAarsakerOgForklaring(request.aarsaker, request.forklaring)
                }
                    .flatMap { utbetalingService.besluttDelutbetaling(id, request, navIdent) }
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }
    }
}

@Serializable
data class BesluttTotrinnskontrollRequest<T>(
    val besluttelse: Besluttelse,
    val aarsaker: List<T>,
    val forklaring: String?,
)

@Serializable
data class DelutbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    val belop: Int,
    val gjorOppTilsagn: Boolean,
)

@Serializable
data class OpprettDelutbetalingerRequest(
    @Serializable(with = UUIDSerializer::class)
    val utbetalingId: UUID,
    val delutbetalinger: List<DelutbetalingRequest>,
    val begrunnelseMindreBetalt: String?,
)

@Serializable
data class OpprettUtbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val beskrivelse: String,
    val kontonummer: Kontonummer,
    val kidNummer: String? = null,
    val belop: Int,
)

data class BeregningFilter(
    val navEnheter: List<NavEnhetNummer>,
)

fun RoutingContext.getBeregningFilter() = BeregningFilter(
    navEnheter = call.parameters.getAll("navEnheter")?.map { NavEnhetNummer(it) } ?: emptyList(),
)
