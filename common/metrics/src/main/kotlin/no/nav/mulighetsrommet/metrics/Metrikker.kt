package no.nav.mulighetsrommet.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

object Metrikker {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    /**
     * Custom metrikk for å registrere HTTP status fra responser vi får fra http-kall
     */
    fun clientResponseMetrics(service: String, status: Int): Counter {
        return Counter.builder("http_client.response").tag("service", service).tag("status", status.toString())
            .register(appMicrometerRegistry)
    }
}
