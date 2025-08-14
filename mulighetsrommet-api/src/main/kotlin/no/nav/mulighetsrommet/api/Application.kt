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
import kotlinx.serialization.json.Json
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor
import no.nav.mulighetsrommet.api.plugins.*
import no.nav.mulighetsrommet.api.routes.OpenApiSpec
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
import no.nav.mulighetsrommet.model.PortableTextTypedObject
import no.nav.mulighetsrommet.model.ProblemDetail
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
    configureStatusPages()

    install(OpenApi) {
        outputFormat = OutputFormat.YAML

        pathFilter = { method, urlParts ->
            val url = urlParts.joinToString("/", prefix = "/")
            OpenApiSpec.match(url) != null
        }

        specAssigner = { url, tags ->
            OpenApiSpec.match(url)?.specName ?: throw IllegalStateException("Failed to resolve OpenApiSpec for $url")
        }

        schemas {
            generator = SchemaGenerator.kotlinx(
                Json {
                    /**
                     * Hvis vi setter denne til true så vil generert skjema for nullable felter ha "null" som et valg,
                     * f.eks. allOf: ["null", "string"] og property blir samtidig markert som required.
                     */
                    explicitNulls = true
                    /**
                     * Gjør at felter som har en default-verdi i Kotlin blir markert som required i skjemaet.
                     */
                    encodeDefaults = true
                },
            ) {
                /**
                 * Gjør at generert skjema ikke har "null" som et valg for nullable felter - altså at property
                 * ikke eksplisitt kan settes til null, men må tas ut av f.eks. en request.
                 *
                 * Default er true, noe som gjør at skjema blir generert med både f.eks. allOf: ["null", "string"]
                 * og at property ikke er required.
                 */
                //  explicitNullTypes = false

                referencePath = RefType.SIMPLE
                title = null

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
                overwrite(
                    SchemaOverwriteModule(
                        identifier = "kotlin.ByteArray",
                        schema = {
                            Schema<Any>().also {
                                it.types = setOf("string")
                                it.format = "binary"
                            }
                        },
                    ),
                )
                overwrite(
                    SchemaOverwriteModule(
                        identifier = "PortableTextTypedObject",
                        schema = {
                            Schema<PortableTextTypedObject>().apply {
                                type = "object"
                                addProperty(
                                    "_type",
                                    Schema<String>().apply {
                                        types = setOf("string")
                                    },
                                )
                                addProperty(
                                    "_key",
                                    Schema<String>().apply {
                                        types = setOf("string")
                                    },
                                )
                                required = listOf("_type")
                                additionalProperties = Schema<Any>()
                            }
                        },
                    ),
                )
                overwrite(
                    SchemaOverwriteModule(
                        identifier = "ProblemDetail",
                        schema = {
                            Schema<ProblemDetail>().apply {
                                type = "object"
                                addProperty(
                                    "type",
                                    Schema<String>().apply {
                                        types = setOf("string")
                                    },
                                )
                                addProperty(
                                    "title",
                                    Schema<String>().apply {
                                        types = setOf("string")
                                    },
                                )
                                addProperty(
                                    "status",
                                    Schema<String>().apply {
                                        types = setOf("integer")
                                    },
                                )
                                addProperty(
                                    "detail",
                                    Schema<String>().apply {
                                        types = setOf("string")
                                    },
                                )
                                addProperty(
                                    "instance",
                                    Schema<String>().apply {
                                        types = setOf("string")
                                    },
                                )
                                required = listOf("type", "title", "status", "detail")
                                additionalProperties = Schema<Any>()
                            }
                        },
                    ),
                )
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
