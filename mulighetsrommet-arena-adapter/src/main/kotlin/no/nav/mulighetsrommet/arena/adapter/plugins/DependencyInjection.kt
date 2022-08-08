package no.nav.mulighetsrommet.arena.adapter.plugins

import io.ktor.server.application.*
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.mulighetsrommet.arena.adapter.*
import no.nav.mulighetsrommet.arena.adapter.consumers.SakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakdeltakerEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakgjennomforingEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.arena.adapter.services.TopicService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
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
            services(appConfig.services),
            clients(appConfig.services, tokenClient)
        )
    }
}

private fun consumers(kafkaConfig: KafkaConfig) = module {
    single {
        val consumers = listOf(
            TiltakEndretConsumer(kafkaConfig.getTopic("tiltakendret"), get(), get()),
            TiltakgjennomforingEndretConsumer(kafkaConfig.getTopic("tiltakgjennomforingendret"), get(), get()),
            TiltakdeltakerEndretConsumer(kafkaConfig.getTopic("tiltakdeltakerendret"), get(), get()),
            SakEndretConsumer(kafkaConfig.getTopic("sakendret"), get(), get()),
        )
        ConsumerGroup(consumers)
    }
}

private fun db(databaseConfig: DatabaseConfig) = module(createdAtStart = true) {
    single { Database(databaseConfig) }
}

private fun kafka(kafkaConfig: KafkaConfig, kafkaPreset: Properties) = module {
    single {
        KafkaConsumerOrchestrator(kafkaPreset, get(), get(), get(), kafkaConfig.topics.pollChangesDelayMs)
    }
}

private fun repositories() = module {
    single { EventRepository(get()) }
    single { TopicRepository(get()) }
}

private fun services(services: ServiceConfig): Module = module {
    single { TopicService(get(), get(), services.topicService) }
}

private fun clients(serviceConfig: ServiceConfig, tokenClient: AzureAdMachineToMachineTokenClient) = module {
    single {
        MulighetsrommetApiClient(serviceConfig.mulighetsrommetApi.url) {
            tokenClient.createMachineToMachineToken(serviceConfig.mulighetsrommetApi.scope)
        }
    }
}
