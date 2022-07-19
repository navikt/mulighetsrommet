package no.nav.mulighetsrommet.arena.adapter

import com.sksamuel.hoplite.Masked
import java.lang.RuntimeException

data class Config(
    val server: ServerConfig,
    val app: AppConfig
)

data class ServerConfig(
    val host: String,
    val port: Int
)

data class AppConfig(
    val enableFailedRecordProcessor: Boolean,
    val services: ServiceConfig,
    val database: DatabaseConfig,
    val kafka: KafkaConfig
)

data class ServiceConfig(
    val mulighetsrommetApi: AuthenticatedService
)

data class AuthenticatedService(
    val url: String,
    val scope: String
)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: Masked
)

data class KafkaConfig(
    val brokers: String,
    val consumerGroupId: String,
    val topics: TopicsConfig
)

fun KafkaConfig.getTopic(key: String): String {
    return topics.consumer.getOrElse(key) {
        throw RuntimeException("No topic configured for key '$key'")
    }
}

data class TopicsConfig(
    val consumer: Map<String, String>
)
