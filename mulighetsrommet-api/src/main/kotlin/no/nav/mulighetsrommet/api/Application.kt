package no.nav.mulighetsrommet.api

import com.github.kagkarlsson.scheduler.Scheduler
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.OutputFormat
import io.github.smiley4.ktoropenapi.config.SchemaGenerator
import io.github.smiley4.ktoropenapi.config.SchemaOverwriteModule
import io.github.smiley4.schemakenerator.swagger.data.RefType
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.swagger.v3.oas.models.media.Schema
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor
import no.nav.mulighetsrommet.api.plugins.*
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
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

fun main() {
    val config = when (NaisEnv.current()) {
        NaisEnv.ProdGCP -> ApplicationConfigProd
        NaisEnv.DevGCP -> ApplicationConfigDev
        NaisEnv.Local -> ApplicationConfigLocal
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
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring({ db.isHealthy() })
    configureSerialization()
    configureStatusPages()

    install(OpenApi) {
        outputFormat = OutputFormat.YAML
        pathFilter = { method, url ->
            url.contains("veilederflate")
        }

        schemas {
            generator = SchemaGenerator.kotlinx {
                overwrite(
                    SchemaOverwriteModule(
                        identifier = "UUIDCustom",
                        schema = {
                            Schema<Any>().also {
                                it.types = setOf("string")
                                it.format = "uuid"
                            }
                        },
                    ),
                )
                overwrite(
                    SchemaOverwriteModule(
                        identifier = "LocalDateTime",
                        schema = {
                            Schema<Any>().also {
                                it.types = setOf("string")
                                it.format = "date-time"
                            }
                        },
                    ),
                )
                overwrite(
                    SchemaOverwriteModule(
                        identifier = "LocalDate",
                        schema = {
                            Schema<Any>().also {
                                it.types = setOf("string")
                                it.format = "date"
                            }
                        },
                    ),
                )
                referencePath = RefType.SIMPLE
                title = null
            }
        }
    }

    FlywayMigrationManager(config.flyway).migrate(db)

    KafkaMetrics(db)
        .withCountStaleConsumerRecords(minutesSinceCreatedAt = 5)
        .withCountStaleProducerRecords(minutesSinceCreatedAt = 1)
        .register(Metrics.micrometerRegistry)

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
