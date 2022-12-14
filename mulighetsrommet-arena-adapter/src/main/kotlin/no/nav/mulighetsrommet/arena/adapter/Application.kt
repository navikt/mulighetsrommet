package no.nav.mulighetsrommet.arena.adapter

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.plugins.configureAuthentication
import no.nav.mulighetsrommet.arena.adapter.plugins.configureDependencyInjection
import no.nav.mulighetsrommet.arena.adapter.plugins.configureHTTP
import no.nav.mulighetsrommet.arena.adapter.plugins.configureSerialization
import no.nav.mulighetsrommet.arena.adapter.routes.apiRoutes
import no.nav.mulighetsrommet.arena.adapter.routes.managerRoutes
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
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
    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()

    val kafka: KafkaConsumerOrchestrator by inject()

    val scheduler: Scheduler by inject()

    routing {
        authenticate {
            apiRoutes()
            managerRoutes()
        }
    }

    environment.monitor.subscribe(ApplicationStarted) {
        if (config.enableFailedRecordProcessor) {
            kafka.enableFailedRecordProcessor()
        }

        scheduler.start()
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()

        scheduler.stop()
    }
}
