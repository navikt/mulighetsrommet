package no.nav.mulighetsrommet.kafka

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.application.Application
import io.ktor.server.config.*
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.mulighetsrommet.kafka.plugins.configureHTTP
import no.nav.mulighetsrommet.kafka.plugins.configureMonitoring
import no.nav.mulighetsrommet.kafka.plugins.configureRouting
import no.nav.mulighetsrommet.kafka.plugins.configureSerialization
import no.nav.mulighetsrommet.kafka.routes.healthRoutes
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {

    val config = ConfigLoader().loadConfigOrThrow<AppConfig>("/application.yaml")

    val server = embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")

            module {
                main()
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
fun Application.main() {
    configureRouting()
    configureSerialization()
    configureMonitoring()
    configureHTTP()

    routing {
        healthRoutes()
    }
}
