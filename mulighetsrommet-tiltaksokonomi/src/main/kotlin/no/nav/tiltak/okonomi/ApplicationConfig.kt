package no.nav.tiltak.okonomi

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import java.util.*

data class AppConfig(
    val httpClientEngine: HttpClientEngine = CIO.create(),
    val server: ServerConfig = ServerConfig(),
    val flyway: FlywayMigrationManager.MigrationConfig = FlywayMigrationManager.MigrationConfig(),
    val database: DatabaseConfig,
    val auth: AuthConfig,
    val kafka: KafkaConfig,
    val clients: ClientConfig,
)

data class KafkaConfig(
    val consumerPropertiesPreset: Properties,
    val producerPropertiesPreset: Properties,
    val topics: KafkaTopics,
    val clients: KafkaClients,
)

data class KafkaTopics(
    val bestillingStatus: String,
    val fakturaStatus: String,
)

data class KafkaClients(
    val okonomiBestillingConsumer: KafkaTopicConsumer.Config,
)

data class ClientConfig(
    val oebsPoAp: AuthenticatedHttpClientConfig,
)

data class AuthConfig(
    val azure: AuthProvider,
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String,
    val tokenEndpointUrl: String,
    val privateJwk: String,
)

data class AuthenticatedHttpClientConfig(
    val url: String,
    val scope: String,
)
