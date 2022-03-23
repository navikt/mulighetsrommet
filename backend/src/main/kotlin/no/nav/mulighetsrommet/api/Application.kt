package no.nav.mulighetsrommet.api

import io.ktor.application.*
import io.ktor.routing.*
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {

    configureDependencyInjection()
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
