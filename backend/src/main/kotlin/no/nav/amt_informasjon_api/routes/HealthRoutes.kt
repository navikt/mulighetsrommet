package no.nav.amt_informasjon_api.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Route.healthRoutes() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    get("/internal/liveness") {
        call.respond(HttpStatusCode.OK)
    }
    get("/internal/readiness") {
//        if (DatabaseFactory.isConnectionEstablished()) {
//            call.respond(HttpStatusCode.OK)
//        } else {
//            call.respond(HttpStatusCode.InternalServerError)
//        }
        // Les kommentar i DatabaseFactory
        call.respond(HttpStatusCode.OK)
    }
    get("/internal/ping") {
        call.respond("PONG")
    }
    get("/internal/prometheus") {
        call.respond(appMicrometerRegistry.scrape())
    }
}
