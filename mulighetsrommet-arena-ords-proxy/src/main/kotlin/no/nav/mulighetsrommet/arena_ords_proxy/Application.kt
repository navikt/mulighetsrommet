package no.nav.mulighetsrommet.arena_ords_proxy

import io.ktor.server.application.*
import no.nav.mulighetsrommet.arena_ords_proxy.plugins.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
}
