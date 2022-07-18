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
import no.nav.mulighetsrommet.arena.adapter.routes.managerRoutes
import no.nav.mulighetsrommet.arena.adapter.services.TopicService
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
    val topicService = TopicService(db)

    val consumers = listOf(
        TiltakEndretConsumer(db, "tiltakendret", app.kafka.getTopic("tiltakendret"), api),
        TiltakgjennomforingEndretConsumer(db, "tiltakgjennomforingendret", app.kafka.getTopic("tiltakgjennomforingendret"), api),
        TiltakdeltakerEndretConsumer(db, "tiltakdeltakerendret", app.kafka.getTopic("tiltakdeltakerendret"), api),
        SakEndretConsumer(db, "sakendret", app.kafka.getTopic("sakendret"), api)
    )

    topicService.upsertConsumerTopics(consumers)

    val kafka = KafkaConsumerOrchestrator(kafkaPreset, db, consumers, topicService)

    initializeServer(server) {
        configure(app, kafka, db, topicService)
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

fun Application.configure(config: AppConfig, kafka: KafkaConsumerOrchestrator, db: Database, topicService: TopicService) {
    configureSerialization()
    configureMonitoring()
    configureHTTP()

    routing {
        internalRoutes(db)
        managerRoutes(topicService)
    }

    environment.monitor.subscribe(ApplicationStarted) {
        if (config.enableFailedRecordProcessor) {
            kafka.enableFailedRecordProcessor()
        }
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
    }
}
