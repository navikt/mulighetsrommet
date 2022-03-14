package no.nav.mulighetsrommet

import io.ktor.server.application.Application
import no.nav.mulighetsrommet.plugins.configureHTTP
import no.nav.mulighetsrommet.plugins.configureMonitoring
import no.nav.mulighetsrommet.plugins.configureRouting
import no.nav.mulighetsrommet.plugins.configureSerialization

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureRouting()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
}
