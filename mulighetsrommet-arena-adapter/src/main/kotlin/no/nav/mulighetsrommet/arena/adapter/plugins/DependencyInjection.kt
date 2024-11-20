package no.nav.mulighetsrommet.arena.adapter.plugins

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.arena.adapter.*
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.clients.TiltakshistorikkClient
import no.nav.mulighetsrommet.arena.adapter.events.ArenaEventConsumer
import no.nav.mulighetsrommet.arena.adapter.events.processors.*
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.NotifyFailedEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.ReplayEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.slack.SlackNotifierImpl
import no.nav.mulighetsrommet.tasks.OpenTelemetrySchedulerListener
import no.nav.mulighetsrommet.tasks.SlackNotifierSchedulerListener
import no.nav.mulighetsrommet.tokenprovider.CachedTokenProvider
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.KoinIsolated
import org.koin.logger.SLF4JLogger

fun Application.configureDependencyInjection(
    appConfig: AppConfig,
) {
    val tokenProvider = CachedTokenProvider.init(appConfig.auth.azure.audience, appConfig.auth.azure.tokenEndpointUrl)
    install(KoinIsolated) {
        SLF4JLogger()
        modules(
            db(appConfig.database),
            kafka(appConfig.kafka),
            repositories(),
            services(appConfig.services, tokenProvider),
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
        ReplayEvents(get(), get())
    }
    single {
        val retryFailedEvents = RetryFailedEvents(tasks.retryFailedEvents, get())
        val notifyFailedEvents = NotifyFailedEvents(get(), get(), get(), tasks.notifyFailedEvents)
        val replayEvents: ReplayEvents = get()

        val db: Database by inject()

        Scheduler
            .create(db.getDatasource(), replayEvents.task)
            .addSchedulerListener(SlackNotifierSchedulerListener(get()))
            .addSchedulerListener(OpenTelemetrySchedulerListener())
            .startTasks(retryFailedEvents.task, notifyFailedEvents.task)
            .build()
    }
}

private fun db(config: DatabaseConfig) = module {
    single<Database>(createdAtStart = true) {
        Database(config)
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
            ArenaEventConsumer(config.consumers.arenaHistTiltakdeltakerEndret, get()),
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
    single { TiltaksgjennomforingRepository(get()) }
    single { ArenaEntityMappingRepository(get()) }
    single { AvtaleRepository(get()) }
}

private fun services(services: ServiceConfig, tokenProvider: CachedTokenProvider): Module = module {
    single {
        MulighetsrommetApiClient(
            config = MulighetsrommetApiClient.Config(maxRetries = 2),
            baseUri = services.mulighetsrommetApi.url,
            tokenProvider = tokenProvider.withScope(services.mulighetsrommetApi.scope),
        )
    }
    single {
        TiltakshistorikkClient(
            config = TiltakshistorikkClient.Config(maxRetries = 2),
            baseUri = services.tiltakshistorikk.url,
            tokenProvider = tokenProvider.withScope(services.tiltakshistorikk.scope),
        )
    }
    single<ArenaOrdsProxyClient> {
        ArenaOrdsProxyClientImpl(
            baseUrl = services.arenaOrdsProxy.url,
            tokenProvider = tokenProvider.withScope(services.arenaOrdsProxy.scope),
        )
    }
    single {
        val processors = listOf(
            SakEventProcessor(get()),
            TiltakEventProcessor(get()),
            AvtaleInfoEventProcessor(get(), get(), get()),
            TiltakgjennomforingEventProcessor(
                get(),
                get(),
                get(),
                config = TiltakgjennomforingEventProcessor.Config(retryUpsertTimes = 10),
            ),
            TiltakshistorikkEventProcessor(get(), get(), get()),
        )
        ArenaEventService(
            config = services.arenaEventService,
            events = get(),
            processors = processors,
            entities = get(),
        )
    }
    single { ArenaEntityService(get(), get(), get(), get(), get()) }
}
