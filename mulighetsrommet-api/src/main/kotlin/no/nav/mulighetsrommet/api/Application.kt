package no.nav.mulighetsrommet.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.internalRoutes
import no.nav.mulighetsrommet.api.routes.swaggerRoutes
import no.nav.mulighetsrommet.api.routes.v1.*
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.ktor.startKtorApplication

fun main() {
    val (server, app) = loadConfiguration<Config>()

    startKtorApplication(server) {
        configure(app)
    }
}

fun Application.configure(config: AppConfig) {
    configureDependencyInjection(config)
    configureAuthentication(config.auth)
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureWebjars()
    configureSentry(NaisEnv.current())

    routing {
        internalRoutes()
        swaggerRoutes()

        authenticate {
            tiltakstypeRoutes()
            tiltaksgjennomforingRoutes()
            innsatsgruppeRoutes()
            arenaRoutes()
            sanityRoutes()
            brukerRoutes()
            frontendLoggerRoutes()
        }
    }
}
