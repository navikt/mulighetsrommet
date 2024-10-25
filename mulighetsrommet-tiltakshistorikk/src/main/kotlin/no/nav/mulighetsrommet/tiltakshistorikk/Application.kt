package no.nav.mulighetsrommet.tiltakshistorikk

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.tiltakshistorikk.clients.TiltakDatadelingClient
import no.nav.mulighetsrommet.tiltakshistorikk.kafka.consumers.AmtDeltakerV1KafkaConsumer
import no.nav.mulighetsrommet.tiltakshistorikk.kafka.consumers.SisteTiltaksgjennomforingerV1KafkaConsumer
import no.nav.mulighetsrommet.tiltakshistorikk.plugins.configureAuthentication
import no.nav.mulighetsrommet.tiltakshistorikk.plugins.configureHTTP
import no.nav.mulighetsrommet.tiltakshistorikk.plugins.configureSerialization
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.GruppetiltakRepository
import no.nav.mulighetsrommet.tokenprovider.CachedTokenProvider
import org.apache.kafka.common.serialization.ByteArrayDeserializer

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
    val db = Database(config.database)

    FlywayMigrationManager(config.flyway).migrate(db)

    configureAuthentication(config.auth)
    configureSerialization()
    configureMonitoring({ db.isHealthy() })
    configureHTTP()

    val gruppetiltakRepository = GruppetiltakRepository(db)
    val deltakerRepository = DeltakerRepository(db)

    val cachedTokenProvider = CachedTokenProvider.init(config.auth.azure.audience, config.auth.azure.tokenEndpointUrl)

    val tiltakDatadelingClient = TiltakDatadelingClient(
        engine = config.httpClientEngine,
        baseUrl = config.clients.tiltakDatadeling.url,
        tokenProvider = cachedTokenProvider.withScope(config.clients.tiltakDatadeling.scope),
    )

    val tiltakshistorikkService = TiltakshistorikkService(deltakerRepository, tiltakDatadelingClient)

    val kafka = configureKafka(config.kafka, db, deltakerRepository, gruppetiltakRepository)

    routing {
        tiltakshistorikkRoutes(deltakerRepository, tiltakshistorikkService)
    }

    environment.monitor.subscribe(ApplicationStarted) {
        kafka.enableFailedRecordProcessor()
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()

        db.close()
    }
}

fun configureKafka(
    config: KafkaConfig,
    db: Database,
    deltakerRepository: DeltakerRepository,
    gruppetiltakRepository: GruppetiltakRepository,
): KafkaConsumerOrchestrator {
    val properties = when (NaisEnv.current()) {
        NaisEnv.Local -> KafkaPropertiesBuilder.consumerBuilder()
            .withBaseProperties()
            .withConsumerGroupId(config.consumerGroupId)
            .withBrokerUrl(config.brokerUrl)
            .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
            .build()

        else -> KafkaPropertiesPreset.aivenDefaultConsumerProperties(config.consumerGroupId)
    }

    val consumers = listOf(
        AmtDeltakerV1KafkaConsumer(
            config = config.consumers.amtDeltakerV1,
            deltakerRepository = deltakerRepository,
        ),
        SisteTiltaksgjennomforingerV1KafkaConsumer(
            config = config.consumers.sisteTiltaksgjennomforingerV1,
            gruppetiltakRepository = gruppetiltakRepository,
        ),
    )

    return KafkaConsumerOrchestrator(
        consumerPreset = properties,
        db = db,
        consumers = consumers,
    )
}
