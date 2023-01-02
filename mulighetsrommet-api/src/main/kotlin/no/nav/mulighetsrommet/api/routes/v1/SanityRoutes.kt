package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.SanityService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.sanityRoutes() {
    val log = LoggerFactory.getLogger(this.javaClass)
    val sanityService: SanityService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/sanity") {
        get {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            val query = call.request.queryParameters["query"]
                ?: return@get call.respondText("No query parameter with value '?query' present. Cannot execute query against Sanity")
            log.debug("Query sanity with value: $query")
            val fnr = when (call.request.queryParameters["fnr"]) {
                "undefined" -> null // Dersom fnr er 'undefined' så trenger vi ikke verdien og det gjør spørringer mot Sanity raskere
                else -> call.request.queryParameters["fnr"]
            }
            val accessToken = call.getAccessToken()

            val result = sanityService.executeQuery(query, fnr, accessToken)
            call.respondText(result.toString(), ContentType.Application.Json)
        }
    }
}
