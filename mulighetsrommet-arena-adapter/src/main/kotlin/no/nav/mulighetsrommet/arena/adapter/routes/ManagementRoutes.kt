package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Route.managementRoutes() {
    get("/assets/*") {
        call.respondRedirect("/management" + call.request.uri)
    }
    singlePageApplication {
        applicationRoute = "management"
        useResources = true
        react("web/dist")
    }
}
