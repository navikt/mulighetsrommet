package no.nav.mulighetsrommet.arena.adapter.plugins

import com.github.kagkarlsson.scheduler.Scheduler
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.server.application.*
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.mulighetsrommet.arena.adapter.*
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.consumers.SakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakdeltakerEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakgjennomforingEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.database.FlywayDatabaseConfig
import no.nav.mulighetsrommet.env.NaisEnv
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

fun Application.configureDependencyInjection(
    appConfig: AppConfig
) {
    val tokenClient = tokenClientProviderForMachineToMachine(appConfig)
    install(Koin) {
        SLF4JLogger()
        modules(
            db(appConfig.database),
            consumers(appConfig.kafka),
            kafka(appConfig.kafka),
            repositories(),
            services(appConfig.services, tokenClient),
            tasks(appConfig.tasks)
        )
    }
}

private fun consumers(kafkaConfig: KafkaConfig) = module {
    single {
        val consumers = listOf(
            TiltakEndretConsumer(kafkaConfig.getTopic("tiltakendret"), get(), get(), get()),
            TiltakgjennomforingEndretConsumer(
                kafkaConfig.getTopic("tiltakgjennomforingendret"),
                get(),
                get(),
                get(),
                get()
            ),
            TiltakdeltakerEndretConsumer(
                kafkaConfig.getTopic("tiltakdeltakerendret"),
                get(),
                get(),
                get(),
                get()
            ),
            SakEndretConsumer(kafkaConfig.getTopic("sakendret"), get(), get())
        )
        ConsumerGroup(consumers)
    }
}

private fun tasks(tasks: TaskConfig) = module {
    single {
        val retryFailedEvents = RetryFailedEvents(tasks.retryFailedEvents, get())

        val db: Database by inject()

        Scheduler
            .create(db.getDatasource())
            .startTasks(retryFailedEvents.toTask())
            .registerShutdownHook()
            .build()
    }
}

private fun db(config: FlywayDatabaseConfig) = module(createdAtStart = true) {
    single<Database> {
        FlywayDatabaseAdapter(config)
    }
}

private fun kafka(kafkaConfig: KafkaConfig) = module {
    single {
        KafkaConsumerOrchestrator(
            createKafkaPreset(kafkaConfig),
            get(),
            get(),
            get(),
            kafkaConfig.topics.pollChangesDelayMs
        )
    }
}

private fun repositories() = module {
    single { ArenaEventRepository(get()) }
    single { TopicRepository(get()) }
    single { TiltakstypeRepository(get()) }
    single { SakRepository(get()) }
    single { DeltakerRepository(get()) }
    single { TiltaksgjennomforingRepository(get()) }
    single { ArenaEntityMappingRepository(get()) }
}

private fun services(services: ServiceConfig, tokenClient: MachineToMachineTokenClient): Module = module {
    single {
        ArenaEventService(services.arenaEventService, get(), get())
    }
    single {
        MulighetsrommetApiClient(
            config = MulighetsrommetApiClient.Config(maxRetries = 5),
            baseUri = services.mulighetsrommetApi.url
        ) {
            tokenClient.createMachineToMachineToken(services.mulighetsrommetApi.scope)
        }
    }
    single<ArenaOrdsProxyClient> {
        ArenaOrdsProxyClientImpl(baseUrl = services.arenaOrdsProxy.url) {
            tokenClient.createMachineToMachineToken(services.arenaOrdsProxy.scope)
        }
    }
    single {
        ArenaEntityService(get(), get(), get(), get(), get(), get())
    }
}

private fun tokenClientProviderForMachineToMachine(config: AppConfig): MachineToMachineTokenClient {
    return when (NaisEnv.current()) {
        NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
            .withClientId(config.auth.azure.audience)
            .withPrivateJwk(createRSAKey("azure").toJSONString())
            .withTokenEndpointUrl(config.auth.azure.tokenEndpointUrl)
            .buildMachineToMachineTokenClient()

        else -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildMachineToMachineTokenClient()
    }
}

private fun createKafkaPreset(config: KafkaConfig): Properties {
    return when (NaisEnv.current()) {
        NaisEnv.Local -> KafkaPropertiesBuilder.consumerBuilder()
            .withBrokerUrl(config.brokers)
            .withBaseProperties()
            .withConsumerGroupId(config.consumerGroupId)
            .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
            .build()

        else -> KafkaPropertiesPreset.aivenDefaultConsumerProperties(config.consumerGroupId)
    }
}

fun createRSAKey(keyID: String): RSAKey = KeyPairGenerator
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
