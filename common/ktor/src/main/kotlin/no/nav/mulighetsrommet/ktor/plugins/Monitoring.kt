package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Application.configureMonitoring() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
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

        callIdMdc("call-id")
    }

    install(CallId) {
        header(HttpHeaders.XRequestId)

        verify { callId ->
            callId.isNotEmpty()
        }
    }
}
