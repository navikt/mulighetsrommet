package no.nav.mulighetsrommet.api.avtale.api

import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.db.RammedetaljerDbo
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerDefaults
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Valuta
import org.koin.ktor.ext.inject
import java.util.UUID
import kotlin.getValue

@Serializable
data class ValutaLongBelop(
    val belop: Long,
    val valuta: Valuta,
)

@Serializable
data class RammedetaljerDto(
    val totalRamme: ValutaLongBelop,
    val utbetaltArena: ValutaLongBelop?,
    val utbetaltTiltaksadmin: List<ValutaLongBelop>,
    val gjenstaendeRamme: ValutaLongBelop,
)

fun RammedetaljerDbo.toDto(utbetaltFraTiltaksadmin: List<ValutaLongBelop>): RammedetaljerDto {
    val tiltaksAdminSum = utbetaltFraTiltaksadmin
        .filter { it.valuta == this.valuta }
        .sumOf { it.belop }
    return RammedetaljerDto(
        totalRamme = ValutaLongBelop(
            belop = totalRamme,
            valuta = valuta,
        ),
        utbetaltArena = utbetaltArena?.let {
            ValutaLongBelop(
                belop = it,
                valuta = valuta,
            )
        },
        utbetaltTiltaksadmin = utbetaltFraTiltaksadmin,
        gjenstaendeRamme = ValutaLongBelop(
            belop = totalRamme - (utbetaltArena ?: 0) - tiltaksAdminSum,
            valuta = valuta,
        ),
    )
}

fun Route.rammedetaljerRoutes() {
    val avtaleService: AvtaleService by inject()
    val db: ApiDatabase by inject()

    route("avtaler/{id}/rammedetaljer") {
        get({
            tags = setOf("Avtale")
            operationId = "hentRammedetaljer"
            request {
                pathParameterUuid("id")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Rammedetaljer for avtale"
                    body<RammedetaljerDto?>()
                }
                code(HttpStatusCode.NoContent) {
                    description = "Avtalen har ikke rammedetaljer"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val id: UUID by call.parameters
            val result = db.session {
                val rammedetaljer = queries.rammedetaljer.get(id) ?: return@session null

                val utbetaltFraTiltaksadmin = queries.delutbetaling.getByAvtale(
                    id,
                    statuser = setOf(
                        DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
                        DelutbetalingStatus.UTBETALT,
                    ),
                )
                    .groupBy { it.pris.valuta }
                    .map { (valuta, delutbetalinger) ->
                        val sum = delutbetalinger.sumOf { it.pris.belop.toLong() }
                        ValutaLongBelop(
                            belop = sum,
                            valuta = valuta,
                        )
                    }
                return@session rammedetaljer.toDto(utbetaltFraTiltaksadmin)
            }

            if (result != null) {
                call.respond(result)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
        authorize(Rolle.AVTALER_SKRIV) {
            put({
                tags = setOf("Avtale")
                operationId = "upsertRammedetaljer"
                request {
                    pathParameterUuid("id")
                    body<RammedetaljerRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Oppdatert rammedetaljer"
                        body<Unit>()
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
                val navIdent = getNavIdent()
                val id: UUID by call.parameters
                val request = call.receive<RammedetaljerRequest>()

                val result = avtaleService.upsertRammedetaljer(id, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }

                call.respondWithStatusResponse(result)
            }

            delete({
                tags = setOf("Avtale")
                operationId = "deleteRammedetaljer"
                request {
                    pathParameterUuid("id")
                }
                response {
                    code(HttpStatusCode.NoContent) {
                        description = "Rammedetaljer er slettet"
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val navIdent = getNavIdent()
                val id: UUID by call.parameters

                avtaleService.deleteRammedetaljer(id, navIdent)

                call.respond(HttpStatusCode.NoContent)
            }

            get(
                "/defaults",
                {
                    tags = setOf("Avtale")
                    operationId = "hentRammedetaljerDefaults"
                    request {
                        pathParameterUuid("id")
                    }
                    response {
                        code(HttpStatusCode.OK) {
                            body<RammedetaljerDefaults>()
                        }
                        default {
                            description = "Problem details"
                            body<ProblemDetail>()
                        }
                    }
                },
            ) {
                val id: UUID by call.parameters
                val result = db.session {
                    val prismodeller = queries.avtale.getOrError(id).prismodeller
                    val rammedetaljer = queries.rammedetaljer.get(id)
                    val valuta = prismodeller.first().valuta
                    RammedetaljerDefaults(
                        valuta,
                        totalRamme = rammedetaljer?.totalRamme ?: 0,
                        utbetaltArena = rammedetaljer?.utbetaltArena,
                    )
                }
                call.respond(result)
            }
        }
    }
}
