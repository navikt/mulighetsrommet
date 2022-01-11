package no.nav.mulighetsrommet.api

import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.ApplicationStopped
import io.ktor.config.HoconApplicationConfig
import io.ktor.routing.routing
import no.nav.mulighetsrommet.api.plugins.configureDependencyInjection
import no.nav.mulighetsrommet.api.plugins.configureHTTP
import no.nav.mulighetsrommet.api.plugins.configureMonitoring
import no.nav.mulighetsrommet.api.plugins.configureRouting
import no.nav.mulighetsrommet.api.plugins.configureSecurity
import no.nav.mulighetsrommet.api.plugins.configureSerialization
import no.nav.mulighetsrommet.api.plugins.configureWebjars
import no.nav.mulighetsrommet.api.routes.healthRoutes
import no.nav.mulighetsrommet.api.routes.innsatsgruppeRoutes
import no.nav.mulighetsrommet.api.routes.swaggerRoutes
import no.nav.mulighetsrommet.api.routes.tiltaksgjennomforingRoutes
import no.nav.mulighetsrommet.api.routes.tiltaksvariantRoutes

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {

    // TODO: Fiks litt bedre config-oppsett for hele appen, sett i app context isteden.
    val appConfig = HoconApplicationConfig(ConfigFactory.load())
    val enableKafka = appConfig.property("ktor.kafka.enable").getString().toBoolean()

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
        tiltaksvariantRoutes()
        tiltaksgjennomforingRoutes()
        innsatsgruppeRoutes()
    }

    // TODO: Lag noe som er litt mer robust. Kun for å få deployet.
    if (enableKafka) {
        // val kafka: KafkaFactory by inject()
        environment.monitor.subscribe(ApplicationStopped) {
            println("Shutting down")
//            kafka.shutdown()
        }
    }
}
