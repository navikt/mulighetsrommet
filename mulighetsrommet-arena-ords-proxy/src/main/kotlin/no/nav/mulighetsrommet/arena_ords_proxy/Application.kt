package no.nav.mulighetsrommet.arena_ords_proxy

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena_ords_proxy.plugins.*
import no.nav.mulighetsrommet.arena_ords_proxy.routes.arenaOrdsRoutes
import no.nav.mulighetsrommet.arena_ords_proxy.routes.internalRoutes
import org.slf4j.LoggerFactory

fun main() {
    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")

    val arenaOrdsClient = ArenaOrdsClient(config.app.ords)

    initializeServer(config, arenaOrdsClient)
}

fun initializeServer(config: Config, arenaOrdsClient: ArenaOrdsClient) {
    val server = embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")

            module {
                configure(arenaOrdsClient)
            }

            connector {
                port = config.server.port
                host = config.server.host
            }
        }
    )
    server.start(true)
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
