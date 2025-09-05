package no.nav.mulighetsrommet.api.navansatt.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.navAnsattRoutes() {
    val ansattService: NavAnsattService by inject()

    route("/ansatt") {
        get({
            tags = setOf("Ansatt")
            operationId = "getAnsatte"
            request {
                queryParameter<List<Rolle>>("roller") {
                    explode = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Nav-ansatte"
                    body<List<NavAnsattDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val filter = getNavAnsattFilter()

            val ansatte = ansattService.getNavAnsatte(filter = filter).map {
                NavAnsattDto.fromNavAnsatt(it)
            }

            call.respond(ansatte)
        }

        get("/sok", {
            tags = setOf("Ansatt")
            operationId = "sokAnsatte"
            request {
                queryParameter<String>("q")
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Nav-ansatte som matcher søket"
                    body<List<NavAnsattDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val q: String by call.request.queryParameters

            val ansatte = ansattService.getNavAnsattFromAzureSok(query = q).map {
                NavAnsattDto.fromEntraNavAnsatt(it)
            }

            call.respond(ansatte)
        }

        get("/me", {
            tags = setOf("Ansatt")
            operationId = "me"
            response {
                code(HttpStatusCode.OK) {
                    description = "Innlogget bruker"
                    body<NavAnsattDto>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val navIdent = getNavIdent()

            val ansatt = ansattService.getNavAnsattByNavIdent(navIdent)
                ?.let { NavAnsattDto.fromNavAnsatt(it) }
                ?: return@get call.respond(HttpStatusCode.NotFound, "Fant ikke ansatt med navIdent=$navIdent")

            call.respond(ansatt)
        }
    }
}

fun RoutingContext.getNavAnsattFilter(): NavAnsattFilter {
    val roller = call.parameters.getAll("roller")
        ?.map { Rolle.valueOf(it) }
        ?: emptyList()

    return NavAnsattFilter(roller = roller)
}

data class NavAnsattFilter(
    val roller: List<Rolle> = emptyList(),
)
