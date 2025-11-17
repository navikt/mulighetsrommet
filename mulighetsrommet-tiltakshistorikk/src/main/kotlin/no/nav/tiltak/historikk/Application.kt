package no.nav.tiltak.historikk

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.util.*
import no.nav.mulighetsrommet.brreg.BrregClient
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
import no.nav.tiltak.historikk.kafka.consumers.SisteTiltaksgjennomforingerV2KafkaConsumer
import no.nav.tiltak.historikk.plugins.configureAuthentication
import no.nav.tiltak.historikk.plugins.configureHTTP
import no.nav.tiltak.historikk.plugins.configureSerialization
import no.nav.tiltak.historikk.service.VirksomhetService
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
            shutdownGracePeriod = 10.seconds.inWholeMilliseconds
            shutdownTimeout = 10.seconds.inWholeMilliseconds
        },
        module = { configure(config) },
    ).start(wait = true)
}

val IsReadyState = AttributeKey<Boolean>("app-is-ready")

fun Application.configure(config: AppConfig) {
    attributes.put(IsReadyState, true)

    configureMetrics()

    val database = configureDatabase(config)

    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ attributes[IsReadyState] }, { database.isHealthy() })
    configureHTTP()

    val texasClient = TexasClient(config.auth.texas, config.auth.texas.engine ?: config.httpClientEngine)
    val azureAdTokenProvider = AzureAdTokenProvider(texasClient)
    val tiltakDatadelingClient = TiltakDatadelingClient(
        engine = config.httpClientEngine,
        baseUrl = config.clients.tiltakDatadeling.url,
        tokenProvider = azureAdTokenProvider.withScope(config.clients.tiltakDatadeling.scope),
    )

    val db = TiltakshistorikkDatabase(database)

    val virksomheter = VirksomhetService(
        db,
        BrregClient(config.httpClientEngine),
    )

    val kafka = configureKafka(config.kafka, db, virksomheter)

    val tiltakshistorikk = TiltakshistorikkService(
        db,
        tiltakDatadelingClient,
        config.arbeidsgiverTiltakCutOffDatoMapping,
    )

    routing {
        tiltakshistorikkRoutes(kafka, db, tiltakshistorikk)
    }

    monitor.subscribe(ApplicationStopPreparing) {
        log.info("ApplicationStopPreparing")
        attributes.put(IsReadyState, false)
    }
}

fun Application.configureDatabase(config: AppConfig): Database {
    val database = Database(config.database)

    FlywayMigrationManager(config.flyway).migrate(database)

    monitor.subscribe(ApplicationStopped) {
        log.info("Closing db...")
        database.close()
    }

    return database
}

fun Application.configureKafka(
    config: KafkaConfig,
    db: TiltakshistorikkDatabase,
    virksomheter: VirksomhetService,
): KafkaConsumerOrchestrator {
    KafkaMetrics(db.db)
        .withCountStaleConsumerRecords(retriesMoreThan = 5)
        .register(Metrics.micrometerRegistry)

    val consumers = mapOf(
        config.consumers.amtDeltakerV1 to AmtDeltakerV1KafkaConsumer(db),
        config.consumers.sisteTiltaksgjennomforingerV1 to SisteTiltaksgjennomforingerV1KafkaConsumer(db),
        config.consumers.sisteTiltaksgjennomforingerV2 to SisteTiltaksgjennomforingerV2KafkaConsumer(db, virksomheter),
    )

    val kafka = KafkaConsumerOrchestrator(
        db = db.db,
        consumers = consumers,
    )

    monitor.subscribe(ApplicationStarted) {
        log.info("Starting kafka consumer record processor")
        kafka.enableFailedRecordProcessor()
    }

    monitor.subscribe(ApplicationStopping) {
        log.info("Stopping kafka consumers...")
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()
    }

    return kafka
}
