package no.nav.mulighetsrommet.api

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor
import no.nav.mulighetsrommet.api.plugins.configureAuthentication
import no.nav.mulighetsrommet.api.plugins.configureDependencyInjection
import no.nav.mulighetsrommet.api.plugins.configureHTTP
import no.nav.mulighetsrommet.api.plugins.configureOpenApiGenerator
import no.nav.mulighetsrommet.api.plugins.configureRouting
import no.nav.mulighetsrommet.api.plugins.configureSecurity
import no.nav.mulighetsrommet.api.plugins.configureSerialization
import no.nav.mulighetsrommet.api.routes.apiRoutes
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.monitoring.KafkaMetrics
import no.nav.mulighetsrommet.ktor.plugins.configureMetrics
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.ktor.plugins.configureStatusPages
import no.nav.mulighetsrommet.metrics.Metrics
import no.nav.mulighetsrommet.teamLogsError
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

fun main() {
    val config = when (NaisEnv.current()) {
        NaisEnv.ProdGCP -> ApplicationConfigProd
        NaisEnv.DevGCP -> ApplicationConfigDev
        NaisEnv.Local -> ApplicationConfigLocal
    }

    createServer(config).start(wait = true)
}

fun createServer(config: AppConfig): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    return embeddedServer(
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
    )
}

fun Application.configure(config: AppConfig) {
    configureMetrics()

    val db by inject<Database>()

    configureDependencyInjection(config)
    configureAuthentication(config.auth)
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring({ db.isHealthy() })
    configureSerialization()
    configureStatusPages(this::logException)
    configureOpenApiGenerator()

    FlywayMigrationManager(config.flyway).migrate(db)

    KafkaMetrics(db)
        .withCountStaleConsumerRecords(retriesMoreThan = 5)
        .withCountStaleProducerRecords(minutesSinceCreatedAt = 1)
        .register(Metrics.micrometerRegistry)

    routing {
        apiRoutes(config)
    }

    val kafka: KafkaConsumerOrchestrator by inject()
    val producerRecordProcessor: KafkaProducerRecordProcessor by inject()
    val shedLockLeaderElectionClient: ShedLockLeaderElectionClient by inject()

    val scheduler: Scheduler by inject()

    monitor.subscribe(ApplicationStarted) {
        log.info("Starting kafka consumer & producer record processor")
        kafka.enableFailedRecordProcessor()
        producerRecordProcessor.start()

        log.info("Started task scheduler")
        scheduler.start()
    }

    monitor.subscribe(ApplicationStopping) {
        log.info("Stopping task scheduler...")
        scheduler.stop()

        log.info("Stopping kafka consumer & producer record processor...")
        producerRecordProcessor.close()
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()
    }

    monitor.subscribe(ApplicationStopped) {
        log.info("Closing Shedlock client...")
        shedLockLeaderElectionClient.close()
        log.info("Closing db...")
        db.close()
    }
}

fun Application.logException(statusCode: HttpStatusCode, cause: Throwable, call: ApplicationCall) {
    val statusDetails = "${statusCode.description} (${statusCode.value})"
    val requestDetails = "${call.request.httpMethod.value} ${call.request.path()}"
    val errorMessage = "$statusDetails on $requestDetails: ${cause.message}"

    log.teamLogsError(errorMessage, cause)

    val summary = "$errorMessage (se stacktrace i Team Logs)"
    when (statusCode.value) {
        in 500..599 -> log.error(summary)
        in 400..499 -> log.warn(summary)
        else -> log.info(summary)
    }
}
