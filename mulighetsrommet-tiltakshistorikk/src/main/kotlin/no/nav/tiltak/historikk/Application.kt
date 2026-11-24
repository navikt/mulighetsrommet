package no.nav.tiltak.historikk

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.ktor.plugins.configureMetrics
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.kafka.consumers.ReplikerAmtDeltakerEksternV1KafkaConsumer
import no.nav.tiltak.historikk.kafka.consumers.ReplikerAmtDeltakerV1KafkaConsumer
import no.nav.tiltak.historikk.kafka.consumers.ReplikerAmtVirksomheterV1KafkaConsumer
import no.nav.tiltak.historikk.kafka.consumers.ReplikerSisteTiltaksgjennomforingerV2KafkaConsumer
import no.nav.tiltak.historikk.kafka.consumers.ReplikerSisteTiltakstyperV3KafkaConsumer
import no.nav.tiltak.historikk.kafka.consumers.ReplikerTiltakAvtaleKafkaConsumer
import no.nav.tiltak.historikk.plugins.configureAuthentication
import no.nav.tiltak.historikk.plugins.configureHTTP
import no.nav.tiltak.historikk.plugins.configureSerialization
import no.nav.tiltak.historikk.replikering.ArbeidsgiverAvtaleReplikering
import no.nav.tiltak.historikk.replikering.ArenaDeltakerReplikering
import no.nav.tiltak.historikk.replikering.ArenaGjennomforingReplikering
import no.nav.tiltak.historikk.replikering.GjennomforingReplikering
import no.nav.tiltak.historikk.replikering.KometDeltakerReplikering
import no.nav.tiltak.historikk.replikering.TiltakstypeReplikering
import no.nav.tiltak.historikk.replikering.VirksomhetReplikering
import no.nav.tiltak.historikk.service.TiltakshistorikkService
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

fun Application.configure(config: AppConfig) {
    configureMetrics()

    val database = configureDatabase(config)

    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ database.isHealthy() })
    configureHTTP()

    val db = TiltakshistorikkDatabase(database)

    val virksomheter = VirksomhetReplikering(db, BrregClient(config.httpClientEngine))

    val kafka = configureKafka(config.kafka, db, virksomheter)

    val tiltakshistorikk = TiltakshistorikkService(
        config = TiltakshistorikkService.Config(
            cutOffDatoMapping = config.arbeidsgiverTiltakCutOffDatoMapping,
            deltakerHistorikkilder = config.deltakerHistorikkilder,
        ),
        db = db,
        virksomheter = virksomheter,
    )

    routing {
        tiltakshistorikkRoutes(
            kafka,
            tiltakshistorikk,
            ArenaDeltakerReplikering(db),
            ArenaGjennomforingReplikering(db, virksomheter),
        )
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
    virksomheter: VirksomhetReplikering,
): KafkaConsumerOrchestrator {
    val consumers = mapOf(
        config.consumers.replikerSisteTiltakstyper to ReplikerSisteTiltakstyperV3KafkaConsumer(
            TiltakstypeReplikering(db),
        ),
        config.consumers.replikerSisteTiltaksgjennomforinger to ReplikerSisteTiltaksgjennomforingerV2KafkaConsumer(
            GjennomforingReplikering(db, virksomheter),
        ),
        config.consumers.replikerAmtDeltaker to ReplikerAmtDeltakerV1KafkaConsumer(
            KometDeltakerReplikering(db),
        ),
        config.consumers.replikerAmtDeltakerEkstern to ReplikerAmtDeltakerEksternV1KafkaConsumer(
            KometDeltakerReplikering(db),
        ),
        config.consumers.replikerAmtVirksomhet to ReplikerAmtVirksomheterV1KafkaConsumer(virksomheter),
        config.consumers.replikerTiltakAvtale to ReplikerTiltakAvtaleKafkaConsumer(
            ArbeidsgiverAvtaleReplikering(db),
        ),
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
