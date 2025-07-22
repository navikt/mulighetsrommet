package no.nav.mulighetsrommet.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Metrics {
    private val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val micrometerRegistry: MeterRegistry
        get() = prometheusRegistry

    fun scrapePrometheusMetrics(): String = prometheusRegistry.scrape()

    /**
     * Custom metrikk for å registrere HTTP status fra responser vi får fra http-kall
     */
    fun clientResponseMetrics(service: String, status: Int): Counter {
        return Counter.builder("http.client.response")
            .tag("service", service)
            .tag("status", status.toString())
            .register(micrometerRegistry)
    }
}
