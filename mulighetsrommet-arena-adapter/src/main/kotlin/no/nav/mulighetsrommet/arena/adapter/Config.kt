package no.nav.mulighetsrommet.arena.adapter

import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.FlywayDatabaseConfig
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig
)

data class AppConfig(
    val enableFailedRecordProcessor: Boolean,
    val tasks: TaskConfig,
    val services: ServiceConfig,
    val database: FlywayDatabaseConfig,
    val kafka: KafkaConfig,
    val auth: AuthConfig,
    val slack: SlackConfig
)

data class TaskConfig(
    val retryFailedEvents: RetryFailedEvents.Config
)

data class ServiceConfig(
    val mulighetsrommetApi: ServiceClientConfig,
    val arenaEventService: ArenaEventService.Config,
    val arenaOrdsProxy: ServiceClientConfig,
)

data class AuthConfig(
    val azure: AuthProvider
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val tokenEndpointUrl: String
)

data class ServiceClientConfig(
    val url: String,
    val scope: String
)

data class KafkaConfig(
    val brokerUrl: String? = null,
    val consumerGroupId: String,
    val topics: TopicsConfig
)

fun KafkaConfig.getTopic(id: String): KafkaTopicConsumer.Config {
    val topic = topics.consumer.getOrElse(id) {
        throw RuntimeException("No topic configured for id '$id'")
    }
    return KafkaTopicConsumer.Config(id, topic)
}

data class TopicsConfig(
    val topicStatePollDelay: Long,
    val consumer: Map<String, String>
)

data class SlackConfig(
    val token: String,
    val channel: String,
    val enable: Boolean
)
