package no.nav.amt_informasjon_api.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
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