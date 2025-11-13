package no.nav.tiltak.okonomi

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.tiltak.okonomi.avstemming.SftpClient
import no.nav.tiltak.okonomi.avstemming.task.DailyAvstemming
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import java.time.LocalDateTime
import java.util.*

data class AppConfig(
    val httpClientEngine: HttpClientEngine = CIO.create(),
    val server: ServerConfig = ServerConfig(),
    val flyway: FlywayMigrationManager.MigrationConfig = FlywayMigrationManager.MigrationConfig(),
    val database: DatabaseConfig,
    val auth: AuthConfig,
    val kafka: KafkaConfig,
    val clients: ClientConfig,
    val avstemming: AvstemmingConfig,
    val slack: SlackConfig,
    val faktura: FakturaConfig,
)

data class FakturaConfig(
    val tidligstTidspunktForUtbetaling: (Bestilling, Faktura) -> LocalDateTime?,
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

data class AvstemmingConfig(
    val sftpProperties: SftpClient.SftpProperties,
    val dailyTask: DailyAvstemming.Config,
)

data class SlackConfig(
    val token: String,
    val channel: String,
    val enable: Boolean,
)
