package no.nav.mulighetsrommet.api.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
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
        filter { call -> call.request.path().startsWith("/") }
        mdc("status") {
            it.response.status().toString()
        }
        mdc("method") {
            it.request.httpMethod.value
        }
    }
}
