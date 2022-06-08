package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.SanityService
import org.koin.ktor.ext.inject

fun Route.sanityRoutes() {

    val sanityService: SanityService by inject()
    val validDatasets = listOf("production", "test", "dev")

    route("/api/v1/sanity") {
        get {
            val query = call.request.queryParameters["query"]
                ?: return@get call.respondText("No query parameter with value '?query' present. Cannot execute query against Sanity")
            val dataset = call.request.queryParameters["dataset"] ?: "production"

            if (!validDatasets.contains(dataset)) throw BadRequestException(
                "Dataset '$dataset' er ikke et gyldig datasett. Gyldige datasett er ${
                validDatasets.joinToString(
                    ", "
                )
                }"
            )

            call.respondText(sanityService.executeQuery(query, dataset), ContentType.Application.Json)
        }
    }
}
