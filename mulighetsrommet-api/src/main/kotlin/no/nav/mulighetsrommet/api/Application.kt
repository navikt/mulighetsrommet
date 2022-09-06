package no.nav.mulighetsrommet.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.internal.*
import no.nav.mulighetsrommet.api.routes.v1.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.ktor.plugins.configureSentry
import no.nav.mulighetsrommet.ktor.startKtorApplication
import org.koin.ktor.ext.inject

fun main() {
    val (server, app) = loadConfiguration<Config>()

    startKtorApplication(server) {
        configure(app)
    }
}

fun Application.configure(config: AppConfig) {
    val db by inject<Database>()

    configureDependencyInjection(config)
    configureAuthentication(config.auth)
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring({ db.isHealthy() })
    configureSerialization()
    configureWebjars(config.swagger)
    configureSentry(config.sentry)

    routing {
        swaggerRoutes()

        authenticate {
            tiltakstypeRoutes()
            tiltaksgjennomforingRoutes()
            innsatsgruppeRoutes()
            arenaRoutes()
            sanityRoutes()
            brukerRoutes()
            veilederRoutes()
            frontendLoggerRoutes()
            dialogRoutes()
        }
    }
}
