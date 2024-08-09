package no.nav.mulighetsrommet.tiltakshistorikk

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig,
)

data class AppConfig(
    val database: DatabaseConfig,
    val flyway: FlywayMigrationManager.MigrationConfig = FlywayMigrationManager.MigrationConfig(),
    val auth: AuthConfig,
    val kafka: KafkaConfig,
)

data class AuthConfig(
    val azure: AuthProvider,
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val tokenEndpointUrl: String,
)

data class KafkaConfig(
    val brokerUrl: String? = null,
    val consumerGroupId: String,
    val consumers: KafkaConsumers,
)

data class KafkaConsumers(
    val amtDeltakerV1: KafkaTopicConsumer.Config,
    val sisteTiltaksgjennomforingerV1: KafkaTopicConsumer.Config,
)
