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
    val config = ConfigLoader().loadConfigOrThrow<AppConfig>("/application.yaml")
    initializeServer(config)
}

fun initializeServer(config: AppConfig) {
    val server = embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")

            module {
                main(config)
            }

            connector {
                port = config.server.port
                host = config.server.host
            }
        }
    )
    server.start(true)
}

fun Application.main(config: AppConfig) {

    configureDependencyInjection(config)
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureWebjars()

    routing {
        healthRoutes()
        swaggerRoutes()
        tiltakstypeRoutes()
        tiltaksgjennomforingRoutes()
        innsatsgruppeRoutes()
    }
}
