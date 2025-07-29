package no.nav.tiltak.okonomi

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.common.kafka.producer.feilhandtering.publisher.QueuedKafkaProducerRecordPublisher
import no.nav.common.kafka.producer.feilhandtering.util.KafkaProducerRecordProcessorBuilder
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.monitoring.KafkaMetrics
import no.nav.mulighetsrommet.ktor.plugins.configureMetrics
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.ktor.plugins.configureStatusPages
import no.nav.mulighetsrommet.metrics.Metrics
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.slack.SlackNotifierImpl
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.OpenTelemetrySchedulerListener
import no.nav.mulighetsrommet.tasks.SlackNotifierSchedulerListener
import no.nav.mulighetsrommet.tokenprovider.AzureAdTokenProvider
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.tiltak.okonomi.api.configureApi
import no.nav.tiltak.okonomi.avstemming.AvstemmingService
import no.nav.tiltak.okonomi.avstemming.SftpClient
import no.nav.tiltak.okonomi.avstemming.task.DailyAvstemming
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.kafka.OkonomiBestillingConsumer
import no.nav.tiltak.okonomi.oebs.OebsPoApClient
import no.nav.tiltak.okonomi.plugins.configureAuthentication
import no.nav.tiltak.okonomi.plugins.configureHTTP
import no.nav.tiltak.okonomi.plugins.configureSerialization
import no.nav.tiltak.okonomi.service.OkonomiService
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

    val db = Database(config.database)

    FlywayMigrationManager(config.flyway).migrate(db)

    KafkaMetrics(db)
        .withCountStaleConsumerRecords(minutesSinceCreatedAt = 5)
        .withCountStaleProducerRecords(minutesSinceCreatedAt = 1)
        .register(Metrics.micrometerRegistry)

    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureStatusPages()
    configureHTTP()

    val texasClient = TexasClient(config.auth.texas, config.auth.texas.engine ?: config.httpClientEngine)
    val azureAdTokenProvider = AzureAdTokenProvider(texasClient)

    val oebs = OebsPoApClient(
        engine = config.httpClientEngine,
        baseUrl = config.clients.oebsPoAp.url,
        tokenProvider = azureAdTokenProvider.withScope(config.clients.oebsPoAp.scope),
    )

    val brreg = BrregClient(config.httpClientEngine)

    val okonomiDb = OkonomiDatabase(db)

    val okonomi = OkonomiService(
        topics = config.kafka.topics,
        db = okonomiDb,
        oebs = oebs,
        brreg = brreg,
    )

    val kafka = configureKafka(config.kafka, db, okonomi)

    configureApi(kafka, okonomi)

    val sftpClient = SftpClient(properties = config.avstemming.sftpProperties)
    val avstemmingService = AvstemmingService(db = okonomiDb, sftpClient)
    val dailyAvstemming = DailyAvstemming(config = config.avstemming.dailyTask, avstemmingService)
    val slackNotifier = SlackNotifierImpl(config.slack.token, config.slack.channel, config.slack.enable)

    configureDbScheduler(
        okonomiDb,
        dailyAvstemming,
        slackNotifier,
    )

    monitor.subscribe(ApplicationStopped) {
        db.close()
    }
}

private fun Application.configureDbScheduler(
    db: OkonomiDatabase,
    dailyAvstemming: DailyAvstemming,
    slackNotifier: SlackNotifier,
) {
    Scheduler
        .create(db.getDatasource())
        .addSchedulerListener(SlackNotifierSchedulerListener(slackNotifier))
        .addSchedulerListener(OpenTelemetrySchedulerListener())
        .startTasks(
            dailyAvstemming.task,
        )
        .serializer(DbSchedulerKotlinSerializer())
        .registerShutdownHook()
        .build()
}

private fun Application.configureKafka(
    config: KafkaConfig,
    db: Database,
    okonomi: OkonomiService,
): KafkaConsumerOrchestrator {
    val producerClient = KafkaProducerClientBuilder.builder<ByteArray, ByteArray?>()
        .withProperties(config.producerPropertiesPreset)
        .build()

    val okonomiDb = OkonomiDatabase(db)
    val shedLockLeaderElectionClient = ShedLockLeaderElectionClient(JdbcLockProvider(db.getDatasource()))
    val repository = object : KafkaProducerRepository {
        override fun storeRecord(record: StoredProducerRecord?): Long {
            error("Not used")
        }

        override fun deleteRecords(ids: List<Long>) {
            return okonomiDb.session { queries.kafkaProducerRecord.deleteRecords(ids) }
        }

        override fun getRecords(maxMessages: Int): List<StoredProducerRecord> {
            return okonomiDb.session { queries.kafkaProducerRecord.getRecords(maxMessages) }
        }

        override fun getRecords(maxMessages: Int, topics: List<String>): List<StoredProducerRecord> {
            return okonomiDb.session { queries.kafkaProducerRecord.getRecords(maxMessages, topics) }
        }
    }
    val producerRecordProcessor = KafkaProducerRecordProcessorBuilder.builder()
        .withProducerRepository(repository)
        .withLeaderElectionClient(shedLockLeaderElectionClient)
        .withRecordPublisher(QueuedKafkaProducerRecordPublisher(producerClient))
        .build()

    val consumers = mapOf(
        config.clients.okonomiBestillingConsumer to OkonomiBestillingConsumer(okonomi),
    )

    val kafkaConsumerOrchestrator = KafkaConsumerOrchestrator(
        db = db,
        consumers = consumers,
    )

    monitor.subscribe(ApplicationStarted) {
        kafkaConsumerOrchestrator.enableFailedRecordProcessor()
        producerRecordProcessor.start()
    }

    monitor.subscribe(ApplicationStopPreparing) {
        kafkaConsumerOrchestrator.disableFailedRecordProcessor()
        kafkaConsumerOrchestrator.stopPollingTopicChanges()
        producerRecordProcessor.close()
        shedLockLeaderElectionClient.close()
    }

    return kafkaConsumerOrchestrator
}
