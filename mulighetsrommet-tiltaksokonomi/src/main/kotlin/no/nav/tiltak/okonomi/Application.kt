package no.nav.tiltak.okonomi

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordProcessor
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.KafkaProducerRepositoryImpl
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.tokenprovider.CachedTokenProvider
import no.nav.tiltak.okonomi.api.configureApi
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.kafka.OkonomiBestillingConsumer
import no.nav.tiltak.okonomi.oebs.OebsPoApClient
import no.nav.tiltak.okonomi.plugins.configureAuthentication
import no.nav.tiltak.okonomi.plugins.configureHTTP
import no.nav.tiltak.okonomi.plugins.configureSerialization
import no.nav.tiltak.okonomi.service.OkonomiService

fun main() {
    val config = when (NaisEnv.current()) {
        NaisEnv.ProdGCP -> throw IllegalStateException("Vi er ikke i prod enda")
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
    val db = Database(config.database)

    FlywayMigrationManager(config.flyway).migrate(db)

    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()

    val cachedTokenProvider = CachedTokenProvider.init(
        config.auth.azure.audience,
        config.auth.azure.tokenEndpointUrl,
        config.auth.azure.privateJwk,
    )

    val oebs = OebsPoApClient(
        engine = config.httpClientEngine,
        baseUrl = config.clients.oebsPoAp.url,
        tokenProvider = cachedTokenProvider.withScope(config.clients.oebsPoAp.scope),
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

    configureApi(kafka, okonomiDb, okonomi)

    monitor.subscribe(ApplicationStopped) {
        db.close()
    }
}

private fun Application.configureKafka(
    config: KafkaConfig,
    db: Database,
    okonomi: OkonomiService,
): KafkaConsumerOrchestrator {
    val bestilling = OkonomiBestillingConsumer(
        config = config.clients.okonomiBestillingConsumer,
        okonomi = okonomi,
    )

    val producerClient = KafkaProducerClientBuilder.builder<ByteArray, ByteArray?>()
        .withProperties(config.producerPropertiesPreset)
        .build()
    val shedLockLeaderElectionClient = ShedLockLeaderElectionClient(JdbcLockProvider(db.getDatasource()))
    val producerRecordProcessor = KafkaProducerRecordProcessor(
        KafkaProducerRepositoryImpl(db),
        producerClient,
        shedLockLeaderElectionClient,
    )

    val kafkaConsumerOrchestrator = KafkaConsumerOrchestrator(
        consumerPreset = config.consumerPropertiesPreset,
        db = db,
        consumers = listOf(bestilling),
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
