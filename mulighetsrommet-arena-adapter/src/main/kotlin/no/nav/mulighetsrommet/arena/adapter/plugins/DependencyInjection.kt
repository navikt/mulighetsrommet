package no.nav.mulighetsrommet.arena.adapter.plugins

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.server.application.*
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
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
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger
import java.util.*

fun Application.configureDependencyInjection(
    appConfig: AppConfig,
    kafkaPreset: Properties,
    tokenClient: AzureAdMachineToMachineTokenClient
) {
    install(Koin) {
        SLF4JLogger()
        modules(
            db(appConfig.database),
            consumers(appConfig.kafka),
            kafka(appConfig.kafka, kafkaPreset),
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

private fun db(databaseConfig: DatabaseConfig) = module(createdAtStart = true) {
    single<Database> {
        FlywayDatabaseAdapter(
            databaseConfig,
            FlywayDatabaseAdapter.MigrationConfig(
                strategy = FlywayDatabaseAdapter.InitializationStrategy.Migrate
            )
        )
    }
}

private fun kafka(kafkaConfig: KafkaConfig, kafkaPreset: Properties) = module {
    single {
        KafkaConsumerOrchestrator(kafkaPreset, get(), get(), get(), kafkaConfig.topics.pollChangesDelayMs)
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

private fun services(services: ServiceConfig, tokenClient: AzureAdMachineToMachineTokenClient): Module = module {
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
