package no.nav.mulighetsrommet.arena.adapter.plugins

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.client.engine.*
import io.ktor.server.application.*
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
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.slack.SlackNotifierImpl
import no.nav.mulighetsrommet.tasks.OpenTelemetrySchedulerListener
import no.nav.mulighetsrommet.tasks.SlackNotifierSchedulerListener
import no.nav.mulighetsrommet.tokenprovider.AzureAdTokenProvider
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.KoinIsolated
import org.koin.logger.SLF4JLogger

fun Application.configureDependencyInjection(
    appConfig: AppConfig,
) {
    val texasClient = TexasClient(appConfig.auth.texas, appConfig.auth.texas.engine ?: appConfig.engine)
    val tokenProvider = AzureAdTokenProvider(texasClient)

    install(KoinIsolated) {
        SLF4JLogger()
        modules(
            db(appConfig.database),
            kafka(appConfig.kafka),
            repositories(),
            services(tokenProvider, appConfig),
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
            .registerShutdownHook()
            .build()
    }
}

private fun db(config: DatabaseConfig) = module {
    single<Database>(createdAtStart = true) {
        Database(config)
    }
}

private fun kafka(config: KafkaConfig) = module {
    single {
        val consumers = mapOf(
            config.consumers.arenaTiltakEndret to ArenaEventConsumer(get()),
            config.consumers.arenaTiltakgjennomforingEndret to ArenaEventConsumer(get()),
            config.consumers.arenaTiltakdeltakerEndret to ArenaEventConsumer(get()),
            config.consumers.arenaHistTiltakdeltakerEndret to ArenaEventConsumer(get()),
            config.consumers.arenaSakEndret to ArenaEventConsumer(get()),
            config.consumers.arenaAvtaleInfoEndret to ArenaEventConsumer(get()),
        )
        KafkaConsumerOrchestrator(
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

private fun services(tokenProvider: AzureAdTokenProvider, config: AppConfig): Module = module {
    val engine = config.engine
    val services = config.services

    single {
        MulighetsrommetApiClient(
            config = MulighetsrommetApiClient.Config(maxRetries = 2),
            baseUri = services.mulighetsrommetApi.url,
            tokenProvider = tokenProvider.withScope(services.mulighetsrommetApi.scope),
            engine = engine,
        )
    }
    single {
        TiltakshistorikkClient(
            config = TiltakshistorikkClient.Config(maxRetries = 2),
            baseUri = services.tiltakshistorikk.url,
            tokenProvider = tokenProvider.withScope(services.tiltakshistorikk.scope),
            engine = engine,
        )
    }
    single<ArenaOrdsProxyClient> {
        ArenaOrdsProxyClientImpl(
            baseUrl = services.arenaOrdsProxy.url,
            tokenProvider = tokenProvider.withScope(services.arenaOrdsProxy.scope),
            engine = engine,
        )
    }
    single {
        val processors = listOf(
            SakEventProcessor(get()),
            TiltakEventProcessor(get()),
            AvtaleInfoEventProcessor(get(), get(), get()),
            TiltakgjennomforingEventProcessor(
                config = TiltakgjennomforingEventProcessor.Config(
                    retryUpsertTimes = 10,
                    tiltakskoder = config.migrering.tiltakskoder,
                ),
                get(),
                get(),
                get(),
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
