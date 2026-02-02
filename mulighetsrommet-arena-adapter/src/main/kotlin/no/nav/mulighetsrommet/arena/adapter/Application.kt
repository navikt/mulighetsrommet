package no.nav.mulighetsrommet.arena.adapter

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
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
import no.nav.mulighetsrommet.ktor.plugins.configureMetrics
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.metrics.Metrics
import org.koin.ktor.ext.inject
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

fun main() {
    val config = when (NaisEnv.current()) {
        NaisEnv.Local -> ApplicationConfigLocal
        NaisEnv.DevGCP -> ApplicationConfigDev
        NaisEnv.ProdGCP -> ApplicationConfigProd
    }

    embeddedServer(
        Netty,
        configure = {
            connector {
                port = config.server.port
                host = config.server.host
            }
            shutdownGracePeriod = 5.seconds.inWholeMilliseconds
            shutdownTimeout = 10.seconds.inWholeMilliseconds
        },
        module = { configure(config) },
    ).start(wait = true)
}

fun Application.configure(config: AppConfig) {
    configureMetrics()

    val db by inject<Database>()

    configureDependencyInjection(config)
    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()

    FlywayMigrationManager(config.flyway).migrate(db)

    KafkaMetrics(db)
        .withCountStaleConsumerRecords(retriesMoreThan = 5)
        .register(Metrics.micrometerRegistry)

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
            log.info("Starting kafka consumer record processor")
            kafka.enableFailedRecordProcessor()
        }

        log.info("Starting task scheduler")
        scheduler.start()

        replayEvents.schedule(Instant.now().plusSeconds(60))
    }

    monitor.subscribe(ApplicationStopping) {
        log.info("Stopping task scheduler...")
        scheduler.stop()

        log.info("Stopping kafka consumers...")
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()
    }

    monitor.subscribe(ApplicationStopped) {
        log.info("Closing db...")
        db.close()
    }
}
