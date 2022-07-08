package no.nav.mulighetsrommet.arena.adapter

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.mulighetsrommet.arena.adapter.consumers.*
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.plugins.configureHTTP
import no.nav.mulighetsrommet.arena.adapter.plugins.configureMonitoring
import no.nav.mulighetsrommet.arena.adapter.plugins.configureSerialization
import no.nav.mulighetsrommet.arena.adapter.routes.internalRoutes
import no.nav.mulighetsrommet.arena.adapter.routes.managementRoutes
import org.slf4j.LoggerFactory

fun main() {
    val (server, app) = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")

    val tokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildMachineToMachineTokenClient()

    val api = MulighetsrommetApiClient(app.services.mulighetsrommetApi.url) {
        tokenClient.createMachineToMachineToken(app.services.mulighetsrommetApi.scope)
    }

    val kafkaPreset = KafkaPropertiesPreset.aivenDefaultConsumerProperties(app.kafka.consumerGroupId)

    val db = Database(app.database)

    val consumers = listOf(
        TiltakEndretConsumer(db, app.kafka.getTopic("tiltakendret"), api),
        TiltakgjennomforingEndretConsumer(db, app.kafka.getTopic("tiltakgjennomforingendret"), api),
        TiltakdeltakerEndretConsumer(db, app.kafka.getTopic("tiltakdeltakerendret"), api),
        SakEndretConsumer(db, app.kafka.getTopic("sakendret"), api)
    )

    val kafka = KafkaConsumerOrchestrator(kafkaPreset, db, consumers)

    initializeServer(server) {
        configure(app, kafka, db)
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

fun Application.configure(config: AppConfig, kafka: KafkaConsumerOrchestrator, db: Database) {
    configureSerialization()
    configureMonitoring()
    configureHTTP()

    routing {
        internalRoutes(db)
        managementRoutes()
    }

    environment.monitor.subscribe(ApplicationStarted) {
        if (config.enableKafkaTopicConsumption) {
            kafka.enableTopicConsumption()
        }
        if (config.enableFailedRecordProcessor) {
            kafka.enableFailedRecordProcessor()
        }
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableTopicConsumption()
        kafka.disableFailedRecordProcessor()
    }
}
