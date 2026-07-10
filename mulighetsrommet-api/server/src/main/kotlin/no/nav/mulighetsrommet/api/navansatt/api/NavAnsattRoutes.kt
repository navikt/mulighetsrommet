package no.nav.mulighetsrommet.api.navansatt.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDto
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDtoQuery
import no.nav.mulighetsrommet.api.clients.msgraph.EntraNavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.navAnsattRoutes() {
    val ansattService: NavAnsattService by inject()
    val navAnsattDtoQuery: NavAnsattDtoQuery by inject()

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
            val roller = filter.roller.map { NavAnsattRolle.generell(it) }

            val ansatte = navAnsattDtoQuery.getAll(rollerContainsAll = roller)

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
                it.toNavAnsattDto()
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

            val ansatt = navAnsattDtoQuery.get(navIdent)
                ?: return@get call.respondWithProblemDetail(NotFound("Fant ikke ansatt med navIdent=$navIdent"))

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

private fun EntraNavAnsatt.toNavAnsattDto(): NavAnsattDto = NavAnsattDto(
    navIdent = navIdent,
    fornavn = fornavn,
    etternavn = etternavn,
    hovedenhet = NavAnsattDto.Hovedenhet(
        enhetsnummer = hovedenhetKode,
        navn = hovedenhetNavn,
    ),
    mobilnummer = mobilnummer,
    epost = epost,
    roller = listOf(),
)
