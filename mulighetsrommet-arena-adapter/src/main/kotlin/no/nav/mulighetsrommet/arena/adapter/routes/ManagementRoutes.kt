package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.server.application.call
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.managementRoutes() {
    get("/assets/*") {
        call.respondRedirect("/manager" + call.request.uri)
    }
    singlePageApplication {
        applicationRoute = "manager"
        useResources = true
        react("web/dist")
    }
}
