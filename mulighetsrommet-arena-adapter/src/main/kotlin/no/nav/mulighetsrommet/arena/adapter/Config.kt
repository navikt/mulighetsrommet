package no.nav.mulighetsrommet.arena.adapter

import no.nav.mulighetsrommet.arena.adapter.services.TopicService
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.ktor.plugins.SentryConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig
)

data class AppConfig(
    val enableFailedRecordProcessor: Boolean,
    val services: ServiceConfig,
    val database: DatabaseConfig,
    val kafka: KafkaConfig,
    val sentry: SentryConfig? = null
)

data class ServiceConfig(
    val mulighetsrommetApi: AuthenticatedService,
    val topicService: TopicService.Config
)

data class AuthenticatedService(
    val url: String,
    val scope: String
)

data class KafkaConfig(
    val brokers: String,
    val consumerGroupId: String,
    val topics: TopicsConfig
)

fun KafkaConfig.getTopic(id: String): ConsumerConfig {
    val topic = topics.consumer.getOrElse(id) {
        throw RuntimeException("No topic configured for id '$id'")
    }
    return ConsumerConfig(id, topic)
}

data class TopicsConfig(
    val pollChangesDelayMs: Long,
    val consumer: Map<String, String>
)

data class ConsumerConfig(
    val id: String,
    val topic: String,
    val initialRunningState: Boolean = false,
)
