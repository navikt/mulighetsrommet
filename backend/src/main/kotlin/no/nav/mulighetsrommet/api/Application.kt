package no.nav.mulighetsrommet.api

import io.ktor.application.*
import io.ktor.routing.*
import no.nav.mulighetsrommet.api.kafka.KafkaFactory
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.*
import org.koin.ktor.ext.inject

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {

    // TODO: Fiks litt bedre config-oppsett for hele appen, sett i app context isteden.
    val enableKafka = environment.config.property("ktor.kafka.enable").getString().toBoolean()

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

    // TODO: Lag noe som er litt mer robust. Kun for å få deployet.
    log.info("Kafka enabled=$enableKafka")
    if (enableKafka) {
        val kafka: KafkaFactory by inject()
        environment.monitor.subscribe(ApplicationStopped) {
            println("Shutting down")
            kafka.stopClient()
        }
    }
}
