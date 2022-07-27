package no.nav.mulighetsrommet.arena.adapter

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.server.application.*
import io.ktor.server.routing.*
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.mulighetsrommet.arena.adapter.consumers.SakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakdeltakerEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakgjennomforingEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.plugins.configureHTTP
import no.nav.mulighetsrommet.arena.adapter.plugins.configureMonitoring
import no.nav.mulighetsrommet.arena.adapter.plugins.configureSentry
import no.nav.mulighetsrommet.arena.adapter.plugins.configureSerialization
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.arena.adapter.routes.apiRoutes
import no.nav.mulighetsrommet.arena.adapter.routes.internalRoutes
import no.nav.mulighetsrommet.arena.adapter.routes.managerRoutes
import no.nav.mulighetsrommet.arena.adapter.services.TopicService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.ktor.startKtorApplication
import java.util.*

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

    startKtorApplication(server) {
        configure(app, kafkaPreset, db, api)
    }
}

fun Application.configure(config: AppConfig, kafkaPreset: Properties, db: Database, api: MulighetsrommetApiClient) {
    val events = EventRepository(db)

    val consumers = listOf(
        TiltakEndretConsumer(config.kafka.getTopic("tiltakendret"), events, api),
        TiltakgjennomforingEndretConsumer(config.kafka.getTopic("tiltakgjennomforingendret"), events, api),
        TiltakdeltakerEndretConsumer(config.kafka.getTopic("tiltakdeltakerendret"), events, api),
        SakEndretConsumer(config.kafka.getTopic("sakendret"), events, api),
    )

    val kafka = KafkaConsumerOrchestrator(kafkaPreset, db, consumers, TopicRepository(db), config.kafka.topics.pollChangesDelayMs)

    val topicService = TopicService(events, consumers)
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureSentry(config.environment)

    routing {
        internalRoutes(db)
        apiRoutes(topicService)
        managerRoutes(kafka)
    }

    environment.monitor.subscribe(ApplicationStarted) {
        if (config.enableFailedRecordProcessor) {
            kafka.enableFailedRecordProcessor()
        }
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()
    }
}
