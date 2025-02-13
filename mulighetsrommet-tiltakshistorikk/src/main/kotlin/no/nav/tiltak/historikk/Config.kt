package no.nav.tiltak.historikk

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.tiltak.historikk.clients.Avtale
import java.time.LocalDate

data class Config(
    val server: ServerConfig,
    val app: AppConfig,
)

data class AppConfig(
    val httpClientEngine: HttpClientEngine = CIO.create(),
    val database: DatabaseConfig,
    val flyway: FlywayMigrationManager.MigrationConfig = FlywayMigrationManager.MigrationConfig(),
    val auth: AuthConfig,
    val kafka: KafkaConfig,
    val clients: ClientConfig,
    val arbeidsgiverTiltakCutOffDatoMapping: Map<
        Avtale.Tiltakstype,
        @Serializable(with = LocalDateSerializer::class)
        LocalDate,
        > = emptyMap(),
)

data class ClientConfig(
    val tiltakDatadeling: ServiceClientConfig,
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

data class ServiceClientConfig(
    val url: String,
    val scope: String,
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
