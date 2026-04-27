package no.nav.tiltak.okonomi

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import java.util.Properties

data class AppConfig(
    val httpClientEngine: HttpClientEngine = CIO.create(),
    val server: ServerConfig = ServerConfig(),
    val flyway: FlywayMigrationManager.MigrationConfig = FlywayMigrationManager.MigrationConfig(
        strategy = FlywayMigrationManager.InitializationStrategy.Migrate,
    ),
    val database: DatabaseConfig,
    val auth: AuthConfig,
    val kafka: KafkaConfig,
    val clients: ClientConfig,
    val slack: SlackConfig,
)

data class KafkaConfig(
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
    val texas: TexasClient.Config,
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

data class SlackConfig(
    val token: String,
    val channel: String,
    val enable: Boolean,
)
