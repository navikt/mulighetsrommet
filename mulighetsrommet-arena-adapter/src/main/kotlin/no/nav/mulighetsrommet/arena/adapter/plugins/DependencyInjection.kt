package no.nav.mulighetsrommet.arena.adapter.plugins

import io.ktor.server.application.*
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.mulighetsrommet.arena.adapter.*
import no.nav.mulighetsrommet.arena.adapter.consumers.SakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakdeltakerEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakgjennomforingEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerSetup
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.arena.adapter.services.TopicService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger
import java.util.Properties

fun Application.configureDependencyInjection(appConfig: AppConfig, kafkaPreset: Properties, tokenClient: AzureAdMachineToMachineTokenClient) {

    install(Koin) {
        SLF4JLogger()
        modules(
            db(appConfig.database),
            consumers(appConfig.kafka),
            kafka(appConfig.kafka, kafkaPreset),
            repositories(),
            services(),
            clients(appConfig.services, tokenClient)
        )
    }
}

private fun consumers(kafkaConfig: KafkaConfig) = module {
    single { ConsumerSetup(kafkaConfig, get(), get()) }
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

private fun services() = module {
    single { TopicService(get(), get()) }
}

private fun clients(serviceConfig: ServiceConfig, tokenClient: AzureAdMachineToMachineTokenClient) = module {
    single {
        MulighetsrommetApiClient(serviceConfig.mulighetsrommetApi.url) {
            tokenClient.createMachineToMachineToken(serviceConfig.mulighetsrommetApi.scope)
        }
    }
}
