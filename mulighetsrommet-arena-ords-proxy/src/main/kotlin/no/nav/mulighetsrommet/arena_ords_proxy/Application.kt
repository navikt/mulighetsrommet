package no.nav.mulighetsrommet.arena_ords_proxy

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena_ords_proxy.plugins.*
import no.nav.mulighetsrommet.arena_ords_proxy.routes.internalRoutes
import org.slf4j.LoggerFactory

fun main() {
    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")
    initializeServer(config)
}

fun initializeServer(config: Config) {
    val server = embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")

            module {
                configure(config.app)
            }

            connector {
                port = config.server.port
                host = config.server.host
            }
        }
    )
    server.start(true)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.configure(appConfig: AppConfig) {
    configureHTTP()
    configureMonitoring()
    configureSerialization()

    routing {
        internalRoutes()
    }
}
