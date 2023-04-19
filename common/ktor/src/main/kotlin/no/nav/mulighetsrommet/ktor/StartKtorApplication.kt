package no.nav.mulighetsrommet.ktor

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

fun startKtorApplication(config: ServerConfig, configure: Application.() -> Unit) {
    val loggerName = configure.javaClass.name.split('$').first()

    val server = embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger(loggerName)

            module(configure)

            connector {
                port = config.port
                host = config.host
            }
        },
    )

    server.start(true)
}
