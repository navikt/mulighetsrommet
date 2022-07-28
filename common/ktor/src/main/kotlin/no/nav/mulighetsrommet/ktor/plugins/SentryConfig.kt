package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.server.application.*
import io.sentry.Sentry
import no.nav.mulighetsrommet.env.NaisEnv

data class SentryConfig(
    val dsn: String,
    val tracesSampleRate: Double? = null,
)

fun Application.configureSentry(config: SentryConfig? = null, env: NaisEnv = NaisEnv.current()) {
    config?.let {
        environment.log.info("Initializing Sentry for environment=$env")

        Sentry.init { options ->
            options.dsn = config.dsn
            options.tracesSampleRate = config.tracesSampleRate
            options.environment = env.clusterName
        }
    }
}
