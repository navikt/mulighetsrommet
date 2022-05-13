package no.nav.mulighetsrommet.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.mulighetsrommet.api.database.Database
import org.koin.ktor.ext.inject

fun Route.internalRoutes() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val db: Database by inject()

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
