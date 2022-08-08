package no.nav.mulighetsrommet.arena_ords_proxy.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Route.internalRoutes() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    get("/internal/liveness") {
        call.respond(HttpStatusCode.OK)
    }

    get("/internal/readiness") {
        call.respond(HttpStatusCode.OK)
    }

    get("/internal/prometheus") {
        call.respond(appMicrometerRegistry.scrape())
    }
}
