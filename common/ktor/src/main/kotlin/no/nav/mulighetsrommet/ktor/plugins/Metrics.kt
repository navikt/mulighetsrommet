package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.metrics.Metrics

/**
 * Eksponerer micrometer-metrikker via Prometheus. Endepunktet må matche path spesifisert i `nais.yaml`.
 *
 * Pluginen burde installeres så tidlig som mulig i applikasjonens livssyklus for å sørge for at det konfigurerte
 * MeterRegistry'et er registrert før andre moduler tar det i bruk.
 */
fun Application.configureMetrics() {
    install(MicrometerMetrics) {
        registry = Metrics.micrometerRegistry
    }

    routing {
        get("/internal/prometheus") {
            call.respond(Metrics.micrometerRegistry.scrape())
        }
    }
}
