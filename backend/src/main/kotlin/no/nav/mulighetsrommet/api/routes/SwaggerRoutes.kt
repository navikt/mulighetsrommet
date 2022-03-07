package no.nav.mulighetsrommet.api.routes

import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.swaggerRoutes() {
    static("static") {
        resources("web")
    }

    get("/swagger-ui") {
        call.respondRedirect("/assets/swagger-ui/index.html?url=/static/openapi.yml")
    }
}
