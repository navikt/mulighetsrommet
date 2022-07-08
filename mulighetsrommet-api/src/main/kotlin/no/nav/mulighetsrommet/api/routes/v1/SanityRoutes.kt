package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.SanityService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.sanityRoutes() {
    val log = LoggerFactory.getLogger(this.javaClass)
    val sanityService: SanityService by inject()

    route("/api/v1/sanity") {
        get {
            val query = call.request.queryParameters["query"]
                ?: return@get call.respondText("No query parameter with value '?query' present. Cannot execute query against Sanity")
            log.debug("Query sanity with value: $query")
            val fnr = call.request.queryParameters["fnr"]
            val accessToken = call.getAccessToken()

            val result = sanityService.executeQuery(query, fnr, accessToken)
            call.respondText(result.toString(), ContentType.Application.Json)
        }
    }
}
