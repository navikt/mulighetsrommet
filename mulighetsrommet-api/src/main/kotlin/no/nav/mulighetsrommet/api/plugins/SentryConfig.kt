package no.nav.mulighetsrommet.api.plugins

import io.ktor.server.application.*
import io.sentry.Sentry

fun Application.configureSentry() {
    Sentry.init { options ->
        options.dsn = "https://658d2a7841654209b04b77e2c179d224@sentry.gc.nav.no/134"
        // Set tracesSampleRate to 1.0 to capture 100% of transactions for performance monitoring.
        // We recommend adjusting this value in production.
        options.tracesSampleRate = 0.8
        // When first trying Sentry it's good to see what the SDK is doing:
        options.isDebug = false
    }
}
