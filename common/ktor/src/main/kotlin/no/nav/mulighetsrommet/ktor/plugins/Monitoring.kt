package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.metrics.Metrikker
import java.util.*

fun interface MonitoredResource {
    fun isAvailable(): Boolean
}

fun Application.configureMonitoring(vararg resources: MonitoredResource) {
    install(MicrometerMetrics) {
        registry = Metrikker.appMicrometerRegistry
    }

    install(CallId) {
        retrieveFromHeader("Nav-Call-Id")
        retrieveFromHeader(HttpHeaders.XRequestId)
        retrieveFromHeader(HttpHeaders.XCorrelationId)

        replyToHeader("Nav-Call-Id")

        generate {
            UUID.randomUUID().toString()
        }

        verify { callId ->
            callId.isNotEmpty()
        }
    }

    install(CallLogging) {
        disableDefaultColors()

        filter { call ->
            call.request.path().startsWith("/internal").not()
        }

        mdc("status") {
            it.response.status().toString()
        }

        mdc("method") {
            it.request.httpMethod.value
        }

        mdc("azp_name") {
            it.principal<JWTPrincipal>()?.get("azp_name")
        }

        callIdMdc("correlationId")
    }

    routing {
        get("/internal/liveness") {
            call.respond(HttpStatusCode.OK)
        }

        get("/internal/readiness") {
            if (resources.all { it.isAvailable() }) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("/internal/prometheus") {
            call.respond(Metrikker.appMicrometerRegistry.scrape())
        }
    }
}
