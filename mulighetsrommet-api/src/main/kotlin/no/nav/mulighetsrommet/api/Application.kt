package no.nav.mulighetsrommet.api

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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
import org.koin.ktor.ext.inject

fun main() {
    val (server, app) = loadConfiguration<Config>()

    embeddedServer(
        Netty,
        port = server.port,
        host = server.host,
        module = { configure(app) },
    ).start(wait = true)
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

    monitor.subscribe(ApplicationStarted) {
        kafka.enableFailedRecordProcessor()

        scheduler.start()
    }

    monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()

        db.close()
    }
}
