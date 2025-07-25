package no.nav.mulighetsrommet.api.lagretfilter

import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.*

fun Route.lagretFilterRoutes() {
    val lagretFilterService: LagretFilterService by inject()

    route("/lagret-filter") {
        post({
            tags = setOf("LagretFilter")
            operationId = "upsertFilter"
            request {
                body<LagretFilter> {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Filteret ble lagret"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val navIdent = getNavIdent()
            val request = call.receive<LagretFilter>()

            lagretFilterService.upsertFilter(brukerId = navIdent.value, request)
                .onLeft {
                    call.respondWithProblemDetail(toProblemDetail(it))
                }
                .onRight {
                    call.respond(HttpStatusCode.OK)
                }
        }

        get("mine/{dokumenttype}", {
            tags = setOf("LagretFilter")
            operationId = "getMineFilterForDokumenttype"
            request {
                pathParameter<LagretFilterType>("dokumenttype") {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Filter for gitt dokumenttype"
                    body<List<LagretFilter>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val navIdent = getNavIdent()
            val dokumenttype: String by call.parameters

            val filter = lagretFilterService.getLagredeFiltereForBruker(
                brukerId = navIdent.value,
                dokumentType = LagretFilterType.valueOf(dokumenttype),
            )

            call.respond(filter)
        }

        delete("{id}", {
            tags = setOf("LagretFilter")
            operationId = "slettLagretFilter"
            request {
                pathParameter<String>("id") {
                    description = "Id til filter som skal slettes"
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Filteret ble slettet"
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val navIdent = getNavIdent()
            val id: UUID by call.parameters

            lagretFilterService.deleteFilter(brukerId = navIdent.value, id)
                .onLeft {
                    call.respondWithProblemDetail(toProblemDetail(it))
                }
                .onRight { filterId ->
                    val status = if (filterId == null) HttpStatusCode.NoContent else HttpStatusCode.OK
                    call.respond(status)
                }
        }
    }
}

private fun toProblemDetail(error: LagretFilterError): ProblemDetail = when (error) {
    is LagretFilterError.Forbidden -> Forbidden(error.message)
}
