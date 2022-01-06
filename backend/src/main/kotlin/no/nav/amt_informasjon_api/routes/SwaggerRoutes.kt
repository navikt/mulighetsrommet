package no.nav.amt_informasjon_api.routes

import io.ktor.application.call
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.swaggerRoutes() {
    static("static") {
        resources("web")
    }

    get("/swagger-ui") {
        call.respondRedirect("/assets/swagger-ui/index.html?url=/static/openapi.yml")
    }
}
