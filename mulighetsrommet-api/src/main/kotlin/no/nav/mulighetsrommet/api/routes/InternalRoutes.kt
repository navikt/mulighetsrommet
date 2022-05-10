package no.nav.mulighetsrommet.api.routes

import io.ktor.http.HttpStatusCode
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
        call.respond(HttpStatusCode.OK)
    }
    get("/internal/ping") {
        call.respond("PONG")
    }
    get("/internal/prometheus") {
        call.respond(appMicrometerRegistry.scrape())
    }
    get("/internal/clean") {
        runCatching {
            db.flyway.clean()
        }.onSuccess {
            call.respondText("Clean successfull", status = HttpStatusCode.OK)
        }.onFailure { call.respondText("Could not clean database", status = HttpStatusCode.InternalServerError) }
    }
    get("/internal/migrate") {
        runCatching {
            db.flyway.migrate()
        }.onSuccess {
            call.respondText("Migrate successfull", status = HttpStatusCode.OK)
        }.onFailure { call.respondText("Could not migrate database", status = HttpStatusCode.InternalServerError) }
    }
}
