package no.nav.mulighetsrommet.arena_ords_proxy

import io.ktor.server.application.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena_ords_proxy.plugins.configureErrorHandling
import no.nav.mulighetsrommet.arena_ords_proxy.plugins.configureHTTP
import no.nav.mulighetsrommet.arena_ords_proxy.plugins.configureMonitoring
import no.nav.mulighetsrommet.arena_ords_proxy.plugins.configureSerialization
import no.nav.mulighetsrommet.arena_ords_proxy.routes.arenaOrdsRoutes
import no.nav.mulighetsrommet.arena_ords_proxy.routes.internalRoutes
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.ktor.startKtorApplication

fun main() {
    val (server, app) = loadConfiguration<Config>()

    val arenaOrdsClient = ArenaOrdsClient(app.ords)

    startKtorApplication(server) {
        configure(arenaOrdsClient)
    }
}

fun Application.configure(arenaOrdsClient: ArenaOrdsClient) {
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureErrorHandling()

    routing {
        internalRoutes()
        arenaOrdsRoutes(arenaOrdsClient)
    }
}
