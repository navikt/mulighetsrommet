package no.nav.mulighetsrommet.arena.adapter.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.mulighetsrommet.arena.adapter.Database

fun Route.internalRoutes(
    db: Database,
) {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    get("/internal/liveness") {
        call.respond(HttpStatusCode.OK)
    }

    get("/internal/readiness") {
        if (db.isHealthy()) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/internal/prometheus") {
        call.respond(appMicrometerRegistry.scrape())
    }
}
