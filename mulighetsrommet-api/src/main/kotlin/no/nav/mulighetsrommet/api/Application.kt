package no.nav.mulighetsrommet.api

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.*
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

fun Application.configure(config: AppConfig) {
    configureDependencyInjection(config)
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureWebjars()

    routing {
        internalRoutes()
        swaggerRoutes()
        tiltakstypeRoutes()
        tiltaksgjennomforingRoutes()
        innsatsgruppeRoutes()
    }
}
