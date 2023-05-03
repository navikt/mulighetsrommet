package no.nav.mulighetsrommet.api.metrics

import io.micrometer.core.instrument.Counter
import no.nav.mulighetsrommet.ktor.plugins.Metrikker

val ErrorCounter = fun(dependency: String): Counter {
    return Counter.builder("errorExternalDeps")
        .description("Teller feil fra eksterne avhengigheter")
        .tag("dependency", dependency)
        .register(Metrikker.appMicrometerRegistry)
}
