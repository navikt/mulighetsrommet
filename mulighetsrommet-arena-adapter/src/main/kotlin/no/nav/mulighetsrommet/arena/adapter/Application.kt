package no.nav.mulighetsrommet.arena.adapter

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena.adapter.plugins.configureAuthentication
import no.nav.mulighetsrommet.arena.adapter.plugins.configureDependencyInjection
import no.nav.mulighetsrommet.arena.adapter.plugins.configureHTTP
import no.nav.mulighetsrommet.arena.adapter.plugins.configureSerialization
import no.nav.mulighetsrommet.arena.adapter.routes.apiRoutes
import no.nav.mulighetsrommet.arena.adapter.routes.managerRoutes
import no.nav.mulighetsrommet.arena.adapter.tasks.ReplayEvents
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.monitoring.KafkaMetrics
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.metrics.Metrikker
import org.koin.ktor.ext.inject
import java.time.Instant

fun main() {
    val config = when (NaisEnv.current()) {
        NaisEnv.Local -> ApplicationConfigLocal
        NaisEnv.DevGCP -> ApplicationConfigDev
        NaisEnv.ProdGCP -> ApplicationConfigProd
    }

    embeddedServer(
        Netty,
        port = config.server.port,
        host = config.server.host,
        module = { configure(config.app) },
    ).start(wait = true)
}

fun Application.configure(config: AppConfig) {
    val db by inject<Database>()

    configureDependencyInjection(config)
    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()

    FlywayMigrationManager(config.flyway).migrate(db)

    KafkaMetrics(db)
        .withCountStaleConsumerRecords(minutesSinceCreatedAt = 5)
        .register(Metrikker.appMicrometerRegistry)

    val kafka: KafkaConsumerOrchestrator by inject()

    val scheduler: Scheduler by inject()

    val replayEvents: ReplayEvents by inject()

    routing {
        authenticate {
            apiRoutes()
            managerRoutes()
        }
    }

    monitor.subscribe(ApplicationStarted) {
        if (config.enableFailedRecordProcessor) {
            kafka.enableFailedRecordProcessor()
        }

        scheduler.start()

        replayEvents.schedule(Instant.now().plusSeconds(60))
    }

    monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()

        db.close()
    }
}
