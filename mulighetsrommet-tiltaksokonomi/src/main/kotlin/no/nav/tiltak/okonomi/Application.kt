package no.nav.tiltak.okonomi

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.tokenprovider.CachedTokenProvider
import no.nav.tiltak.okonomi.api.configureApi
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.kafka.OkonomiBestillingConsumer
import no.nav.tiltak.okonomi.oebs.OebsTiltakApiClient
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

    val okonomiDb = OkonomiDatabase(db)

    val cachedTokenProvider = CachedTokenProvider.init(config.auth.azure.audience, config.auth.azure.tokenEndpointUrl)

    val oebsClient = OebsTiltakApiClient(
        engine = config.httpClientEngine,
        baseUrl = config.clients.oebsTiltakApi.url,
        tokenProvider = cachedTokenProvider.withScope(config.clients.oebsTiltakApi.scope),
    )
    val brreg = BrregClient(config.httpClientEngine)
    val okonomi = OkonomiService(okonomiDb, oebsClient, brreg)
    val kafka = configureKafka(config.kafka, db, okonomi)

    configureApi(kafka, okonomiDb, okonomi)

    monitor.subscribe(ApplicationStarted) {
        kafka.enableFailedRecordProcessor()
    }

    monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()

        db.close()
    }
}

fun configureKafka(
    config: KafkaConfig,
    db: Database,
    okonomi: OkonomiService,
): KafkaConsumerOrchestrator {
    val bestilling = OkonomiBestillingConsumer(
        config = config.clients.okonomiBestillingConsumer,
        okonomi = okonomi,
    )

    return KafkaConsumerOrchestrator(
        consumerPreset = config.consumerPropertiesPreset,
        db = db,
        consumers = listOf(bestilling),
    )
}
