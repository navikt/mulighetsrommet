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
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import org.koin.ktor.ext.inject
import java.time.Instant

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
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()

    FlywayMigrationManager(config.flyway).migrate(db)

    val kafka: KafkaConsumerOrchestrator by inject()

    val scheduler: Scheduler by inject()

    val replayEvents: ReplayEvents by inject()

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

        replayEvents.schedule(Instant.now().plusSeconds(60))
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()

        scheduler.stop()

        db.close()
    }
}
