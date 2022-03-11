package no.nav.mulighetsrommet.kafka

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.mulighetsrommet.kafka.plugins.configureHTTP
import no.nav.mulighetsrommet.kafka.plugins.configureMonitoring
import no.nav.mulighetsrommet.kafka.plugins.configureRouting
import no.nav.mulighetsrommet.kafka.plugins.configureSerialization
import no.nav.mulighetsrommet.kafka.routes.healthRoutes

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureRouting()
    configureSerialization()
    configureMonitoring()
    configureHTTP()

    routing {
        healthRoutes()
    }
}
