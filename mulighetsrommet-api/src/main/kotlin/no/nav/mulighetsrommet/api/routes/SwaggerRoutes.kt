package no.nav.mulighetsrommet.api.routes

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.swaggerRoutes() {
    static("static") {
        resources("web")
    }
    get("/swagger-ui") {
        call.respondRedirect("/assets/swagger-ui/index.html")
    }
}
