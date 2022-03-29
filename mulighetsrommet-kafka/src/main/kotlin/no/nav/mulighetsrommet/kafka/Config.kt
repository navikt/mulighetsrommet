package no.nav.mulighetsrommet.kafka

import com.sksamuel.hoplite.Masked

data class AppConfig(
    val server: ServerConfig,
    val endpoints: Map<String, String>,
    val database: DatabaseConfig,
    val kafka: KafkaConfig
)

data class ServerConfig(
    val host: String,
    val port: Int
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

data class TopicsConfig(
    val consumer: Map<String, String>
)
