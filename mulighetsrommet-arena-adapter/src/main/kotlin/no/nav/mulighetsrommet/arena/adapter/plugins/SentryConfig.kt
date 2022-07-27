package no.nav.mulighetsrommet.arena.adapter.plugins

import io.ktor.server.application.*
import io.sentry.Sentry

fun Application.configureSentry() {
    Sentry.init { options ->
        options.dsn = "https://9b3945d6ab0542bf9ae41247dab65389@sentry.gc.nav.no/135"
        // Set tracesSampleRate to 1.0 to capture 100% of transactions for performance monitoring.
        // We recommend adjusting this value in production.
        options.tracesSampleRate = 0.8
        // When first trying Sentry it's good to see what the SDK is doing:
        options.isDebug = false
    }
}
