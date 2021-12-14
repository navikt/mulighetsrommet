package no.nav.amt_informasjon_api

import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.ApplicationStopped
import io.ktor.config.HoconApplicationConfig
import io.ktor.routing.routing
import no.nav.amt_informasjon_api.kafka.KafkaFactory
import no.nav.amt_informasjon_api.plugins.configureHTTP
import no.nav.amt_informasjon_api.plugins.configureMonitoring
import no.nav.amt_informasjon_api.plugins.configureRouting
import no.nav.amt_informasjon_api.plugins.configureSecurity
import no.nav.amt_informasjon_api.plugins.configureSerialization
import no.nav.amt_informasjon_api.routes.devRoutes
import no.nav.amt_informasjon_api.routes.healthRoutes
import no.nav.amt_informasjon_api.routes.innsatsgruppeRoutes
import no.nav.amt_informasjon_api.routes.tiltaksgjennomforingRoutes
import no.nav.amt_informasjon_api.routes.tiltaksvariantRoutes
import no.nav.amt_informasjon_api.services.InnsatsgruppeService
import no.nav.amt_informasjon_api.services.TiltaksgjennomforingService
import no.nav.amt_informasjon_api.services.TiltaksvariantService

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {

    // TODO: Fiks litt bedre config-oppsett for hele appen, sett i app context isteden.
    val appConfig = HoconApplicationConfig(ConfigFactory.load())
    val enableKafka = appConfig.property("ktor.kafka.enable").getString().toBoolean()
    // val kafka: KafkaFactory

    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()

    val tiltaksvariantService = TiltaksvariantService()
    val tiltaksgjennomforingService = TiltaksgjennomforingService()
    val innsatsgruppeService = InnsatsgruppeService()

    routing {
        devRoutes()
        healthRoutes()

        tiltaksvariantRoutes(tiltaksvariantService, tiltaksgjennomforingService)
        tiltaksgjennomforingRoutes(tiltaksgjennomforingService)
        innsatsgruppeRoutes(innsatsgruppeService)
    }

    // TODO: Lag noe som er litt mer robust. Kun for å få deployet.
    if (enableKafka) {
        val kafkaFactory = KafkaFactory()
        val kafkaBrokers = appConfig.property("ktor.kafka.brokers").getString()
        println("KAFKA_BROKERS: $kafkaBrokers")
        environment.monitor.subscribe(ApplicationStopped) {
            println("Shutting down")
            kafkaFactory.stopClient()
        }
    }
}
