package no.nav.mulighetsrommet.arena.adapter

import io.ktor.server.application.*
import io.ktor.server.routing.*
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.plugins.configureDependencyInjection
import no.nav.mulighetsrommet.arena.adapter.plugins.configureHTTP
import no.nav.mulighetsrommet.arena.adapter.plugins.configureSerialization
import no.nav.mulighetsrommet.arena.adapter.routes.apiRoutes
import no.nav.mulighetsrommet.arena.adapter.routes.managerRoutes
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.ktor.plugins.configureSentry
import no.nav.mulighetsrommet.ktor.startKtorApplication
import org.koin.ktor.ext.inject
import java.util.*

fun main() {
    val (server, app) = loadConfiguration<Config>()

    val kafkaPreset = KafkaPropertiesPreset.aivenDefaultConsumerProperties(app.kafka.consumerGroupId)

    val tokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildMachineToMachineTokenClient()

    startKtorApplication(server) {
        configure(app, kafkaPreset, tokenClient)
    }
}

fun Application.configure(config: AppConfig, kafkaPreset: Properties, tokenClient: AzureAdMachineToMachineTokenClient) {
    val db by inject<Database>()

    configureDependencyInjection(config, kafkaPreset, tokenClient)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()
    configureSentry(config.sentry)

    val kafka: KafkaConsumerOrchestrator by inject()

    routing {
        apiRoutes()
        managerRoutes()
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
