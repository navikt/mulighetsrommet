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
import no.nav.mulighetsrommet.arena.adapter.events.ArenaEventConsumer
import no.nav.mulighetsrommet.arena.adapter.events.processors.*
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.NotifyFailedEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.ReplayEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.slack.SlackNotifierImpl
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.KoinIsolated
import org.koin.logger.SLF4JLogger
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun Application.configureDependencyInjection(
    appConfig: AppConfig,
) {
    val tokenClient = createM2mTokenClient(appConfig)
    install(KoinIsolated) {
        SLF4JLogger()
        modules(
            db(appConfig.database),
            kafka(appConfig.kafka),
            repositories(),
            services(appConfig.services, tokenClient),
            tasks(appConfig.tasks),
            slack(appConfig.slack),
        )
    }
}

fun slack(slack: SlackConfig): Module {
    return module(createdAtStart = true) {
        single<SlackNotifier> {
            SlackNotifierImpl(slack.token, slack.channel, slack.enable)
        }
    }
}

private fun tasks(tasks: TaskConfig) = module {
    single {
        ReplayEvents(get(), get(), get())
    }
    single {
        val retryFailedEvents = RetryFailedEvents(tasks.retryFailedEvents, get(), get())
        val notifyFailedEvents = NotifyFailedEvents(get(), get(), get(), tasks.notifyFailedEvents)
        val replayEvents: ReplayEvents = get()

        val db: Database by inject()

        Scheduler
            .create(db.getDatasource(), replayEvents.task)
            .startTasks(retryFailedEvents.task, notifyFailedEvents.task)
            .registerShutdownHook()
            .build()
    }
}

private fun db(config: FlywayDatabaseAdapter.Config) = module(createdAtStart = true) {
    single<Database> {
        FlywayDatabaseAdapter(config, get())
    }
}

private fun kafka(config: KafkaConfig) = module {
    val properties = when (NaisEnv.current()) {
        NaisEnv.Local -> KafkaPropertiesBuilder.consumerBuilder()
            .withBaseProperties()
            .withConsumerGroupId(config.consumerGroupId)
            .withBrokerUrl(config.brokerUrl)
            .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
            .build()

        else -> KafkaPropertiesPreset.aivenDefaultConsumerProperties(config.consumerGroupId)
    }

    single {
        val consumers = listOf(
            ArenaEventConsumer(config.consumers.arenaTiltakEndret, get()),
            ArenaEventConsumer(config.consumers.arenaTiltakgjennomforingEndret, get()),
            ArenaEventConsumer(config.consumers.arenaTiltakdeltakerEndret, get()),
            ArenaEventConsumer(config.consumers.arenaSakEndret, get()),
            ArenaEventConsumer(config.consumers.arenaAvtaleInfoEndret, get()),
        )
        KafkaConsumerOrchestrator(
            consumerPreset = properties,
            db = get(),
            consumers = consumers,
        )
    }
}

private fun repositories() = module {
    single { ArenaEventRepository(get()) }
    single { TiltakstypeRepository(get()) }
    single { SakRepository(get()) }
    single { DeltakerRepository(get()) }
    single { TiltaksgjennomforingRepository(get()) }
    single { ArenaEntityMappingRepository(get()) }
    single { AvtaleRepository(get()) }
}

private fun services(services: ServiceConfig, tokenClient: MachineToMachineTokenClient): Module = module {
    single {
        MulighetsrommetApiClient(
            config = MulighetsrommetApiClient.Config(maxRetries = 2),
            baseUri = services.mulighetsrommetApi.url,
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
        val processors = listOf(
            TiltakEventProcessor(get()),
            TiltakgjennomforingEventProcessor(
                get(),
                get(),
                get(),
                config = TiltakgjennomforingEventProcessor.Config(retryUpsertTimes = 10),
            ),
            TiltakdeltakerEventProcessor(get(), get(), get()),
            SakEventProcessor(get()),
            AvtaleInfoEventProcessor(get(), get(), get()),
        )
        ArenaEventService(
            config = services.arenaEventService,
            events = get(),
            processors = processors,
            entities = get(),
        )
    }
    single { ArenaEntityService(get(), get(), get(), get(), get(), get()) }
}

private fun createM2mTokenClient(config: AppConfig): MachineToMachineTokenClient {
    return when (NaisEnv.current()) {
        NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
            .withClientId(config.auth.azure.audience)
            .withPrivateJwk(createMockRSAKey("azure").toJSONString())
            .withTokenEndpointUrl(config.auth.azure.tokenEndpointUrl)
            .buildMachineToMachineTokenClient()

        else -> AzureAdTokenClientBuilder.builder().withNaisDefaults().buildMachineToMachineTokenClient()
    }
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
