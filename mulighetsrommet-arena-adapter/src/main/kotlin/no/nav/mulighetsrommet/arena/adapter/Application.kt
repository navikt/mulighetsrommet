package no.nav.mulighetsrommet.arena.adapter

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.mulighetsrommet.arena.adapter.plugins.configureHTTP
import no.nav.mulighetsrommet.arena.adapter.plugins.configureMonitoring
import no.nav.mulighetsrommet.arena.adapter.plugins.configureSerialization
import no.nav.mulighetsrommet.arena.adapter.routes.internalRoutes
import org.slf4j.LoggerFactory

fun main() {
    val (server, app) = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")

    val tokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildMachineToMachineTokenClient()

    val mulighetsrommetApiClient = MulighetsrommetApiClient(app.services.mulighetsrommetApi.url) {
        tokenClient.createMachineToMachineToken(app.services.mulighetsrommetApi.scope)
    }

    val db = Database(app.database)
    val kafkaPreset = KafkaPropertiesPreset.aivenDefaultConsumerProperties(app.kafka.consumerGroupId)
    val kafka = Kafka(app.kafka, kafkaPreset, db, mulighetsrommetApiClient)

    initializeServer(server) {
        configure(app, kafka)
    }
}

fun initializeServer(config: ServerConfig, main: Application.() -> Unit) {
    val server = embeddedServer(
        Netty,
        environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")

            module(main)

            connector {
                port = config.port
                host = config.host
            }
        }
    )
    server.start(true)
}

fun Application.configure(config: AppConfig, kafka: Kafka) {
    configureSerialization()
    configureMonitoring()
    configureHTTP()

    routing {
        internalRoutes()
    }

    environment.monitor.subscribe(ApplicationStarted) {
        if (config.startKafkaTopicConsumption) {
            kafka.startTopicConsumption()
        }
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.stopTopicConsumption()
    }
}
