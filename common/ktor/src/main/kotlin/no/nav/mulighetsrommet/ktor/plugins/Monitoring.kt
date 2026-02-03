package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import java.util.UUID

fun interface MonitoredResource {
    fun isAvailable(): Boolean
}

/**
 * Graceful startup/shutdown indicator
 */
val IsReadyState = AttributeKey<Boolean>("app-is-ready")

fun Application.configureMonitoring(vararg resources: MonitoredResource) {
    attributes.put(IsReadyState, false)

    monitor.subscribe(ApplicationStarted) {
        log.info("Application is ready")
        attributes.put(IsReadyState, true)
    }
    monitor.subscribe(ApplicationStopPreparing) {
        log.info("Application is preparing to stop")
        attributes.put(IsReadyState, false)
    }

    val checkReadyState = { attributes[IsReadyState] }

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
            if (resources.all { it.isAvailable() } && checkReadyState()) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
