package no.nav.mulighetsrommet.api

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.apiRoutes
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.ktor.plugins.configureStatusPagesForStatusException
import no.nav.mulighetsrommet.ktor.startKtorApplication
import org.koin.ktor.ext.inject

fun main() {
    val (server, app) = loadConfiguration<Config>()

    startKtorApplication(server) {
        configure(app)
    }
}

fun Application.configure(config: AppConfig) {
    val db by inject<Database>()

    configureDependencyInjection(config)
    configureAuthentication(config.auth)
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring({ db.isHealthy() })
    configureSerialization()
    configureStatusPagesForStatusException()

    FlywayMigrationManager(config.flyway).migrate(db)

    routing {
        apiRoutes()

        swaggerUI(path = "/swagger-ui/internal", swaggerFile = "web/openapi.yaml")
        swaggerUI(path = "/swagger-ui/external", swaggerFile = "web/openapi-external.yaml")
    }

    val kafka: KafkaConsumerOrchestrator by inject()

    val scheduler: Scheduler by inject()

    environment.monitor.subscribe(ApplicationStarted) {
        kafka.enableFailedRecordProcessor()

        scheduler.start()
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()

        scheduler.stop()

        db.close()
    }
}
