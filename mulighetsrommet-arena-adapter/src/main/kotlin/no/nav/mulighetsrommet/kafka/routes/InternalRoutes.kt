package no.nav.mulighetsrommet.kafka.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
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
    get("/internal/ping") {
        call.respond("PONG")
    }
    get("/internal/prometheus") {
        call.respond(appMicrometerRegistry.scrape())
    }
}
