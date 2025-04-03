package no.nav.mulighetsrommet.api

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.apiRoutes
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.ktor.plugins.configureStatusPages
import org.koin.ktor.ext.inject

fun main() {
    val config = when (NaisEnv.current()) {
        NaisEnv.ProdGCP -> ApplicationConfigProd
        NaisEnv.DevGCP -> ApplicationConfigDev
        NaisEnv.Local -> ApplicationConfigLocal
    }

    embeddedServer(
        Netty,
        port = config.server.port,
        host = config.server.host,
        module = { configure(config) },
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
    configureStatusPages()

    FlywayMigrationManager(config.flyway).migrate(db)

    routing {
        apiRoutes()

        swaggerUI(path = "/swagger-ui/internal", swaggerFile = "web/openapi.yaml")
        swaggerUI(path = "/swagger-ui/external", swaggerFile = "web/openapi-external.yaml")
    }

    val kafka: KafkaConsumerOrchestrator by inject()
    val producerRecordProcessor: KafkaProducerRecordProcessor by inject()
    val shedLockLeaderElectionClient: ShedLockLeaderElectionClient by inject()

    val scheduler: Scheduler by inject()

    monitor.subscribe(ApplicationStarted) {
        kafka.enableFailedRecordProcessor()
        producerRecordProcessor.start()

        scheduler.start()
    }

    monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()
        producerRecordProcessor.close()
        shedLockLeaderElectionClient.close()

        db.close()
    }
}
