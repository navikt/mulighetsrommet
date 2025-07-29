package no.nav.tiltak.historikk

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.monitoring.KafkaMetrics
import no.nav.mulighetsrommet.ktor.plugins.configureMetrics
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.metrics.Metrics
import no.nav.mulighetsrommet.tokenprovider.AzureAdTokenProvider
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.tiltak.historikk.clients.TiltakDatadelingClient
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.kafka.consumers.AmtDeltakerV1KafkaConsumer
import no.nav.tiltak.historikk.kafka.consumers.SisteTiltaksgjennomforingerV1KafkaConsumer
import no.nav.tiltak.historikk.plugins.configureAuthentication
import no.nav.tiltak.historikk.plugins.configureHTTP
import no.nav.tiltak.historikk.plugins.configureSerialization
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
        .register(Metrics.micrometerRegistry)

    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()

    val tiltakshistorikkDb = TiltakshistorikkDatabase(db)

    val texasClient = TexasClient(config.auth.texas, config.auth.texas.engine ?: config.httpClientEngine)
    val azureAdTokenProvider = AzureAdTokenProvider(texasClient)

    val tiltakDatadelingClient = TiltakDatadelingClient(
        engine = config.httpClientEngine,
        baseUrl = config.clients.tiltakDatadeling.url,
        tokenProvider = azureAdTokenProvider.withScope(config.clients.tiltakDatadeling.scope),
    )

    val tiltakshistorikkService = TiltakshistorikkService(
        tiltakshistorikkDb,
        tiltakDatadelingClient,
        config.arbeidsgiverTiltakCutOffDatoMapping,
    )

    val kafka = configureKafka(config.kafka, tiltakshistorikkDb)

    routing {
        tiltakshistorikkRoutes(kafka, tiltakshistorikkDb, tiltakshistorikkService)
    }

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
    db: TiltakshistorikkDatabase,
): KafkaConsumerOrchestrator {
    val consumers = mapOf(
        config.consumers.amtDeltakerV1 to AmtDeltakerV1KafkaConsumer(db),
        config.consumers.sisteTiltaksgjennomforingerV1 to SisteTiltaksgjennomforingerV1KafkaConsumer(db),
    )

    return KafkaConsumerOrchestrator(
        db = db.db,
        consumers = consumers,
    )
}
