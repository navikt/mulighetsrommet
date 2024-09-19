package no.nav.mulighetsrommet.tiltakshistorikk

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.server.application.*
import io.ktor.server.routing.*
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.OnBehalfOfTokenClient
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.hoplite.loadConfiguration
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.ktor.plugins.configureMonitoring
import no.nav.mulighetsrommet.ktor.startKtorApplication
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
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun main() {
    val (server, app) = loadConfiguration<Config>()

    startKtorApplication(server) {
        configure(app)
    }
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

    val cachedTokenProvider = CachedTokenProvider(
        m2mTokenProvider = createM2mTokenClient(config.auth),
        oboTokenProvider = createOboTokenClient(config.auth),
    )

    val tiltakDatadelingClient = TiltakDatadelingClient(
        engine = config.httpClientEngine,
        baseUrl = config.tiltakDatadeling.url,
        tokenProvider = cachedTokenProvider.withScope(config.tiltakDatadeling.scope),
    )

    val kafka = configureKafka(config.kafka, db, deltakerRepository, gruppetiltakRepository)

    routing {
        tiltakshistorikkRoutes(deltakerRepository, tiltakDatadelingClient)
    }

    environment.monitor.subscribe(ApplicationStarted) {
        kafka.enableFailedRecordProcessor()
    }

    environment.monitor.subscribe(ApplicationStopPreparing) {
        kafka.disableFailedRecordProcessor()
        kafka.stopPollingTopicChanges()
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

private fun createOboTokenClient(authConfig: AuthConfig): OnBehalfOfTokenClient = when (NaisEnv.current()) {
    NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
        .withClientId(authConfig.azure.audience)
        .withPrivateJwk(createMockRSAKey("azure").toJSONString())
        .withTokenEndpointUrl(authConfig.azure.tokenEndpointUrl)
        .buildOnBehalfOfTokenClient()

    else -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildOnBehalfOfTokenClient()
}

private fun createM2mTokenClient(authConfig: AuthConfig): MachineToMachineTokenClient = when (NaisEnv.current()) {
    NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
        .withClientId(authConfig.azure.audience)
        .withPrivateJwk(createMockRSAKey("azure").toJSONString())
        .withTokenEndpointUrl(authConfig.azure.tokenEndpointUrl)
        .buildMachineToMachineTokenClient()

    else -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildMachineToMachineTokenClient()
}

private fun createMockRSAKey(keyID: String): RSAKey = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
    }
