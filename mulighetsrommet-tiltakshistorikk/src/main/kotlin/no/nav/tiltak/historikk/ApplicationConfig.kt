package no.nav.tiltak.historikk

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.tiltak.historikk.clients.Avtale
import java.time.LocalDate
import java.util.Properties

data class AppConfig(
    val server: ServerConfig = ServerConfig(),
    val httpClientEngine: HttpClientEngine = CIO.create(),
    val database: DatabaseConfig,
    val flyway: FlywayMigrationManager.MigrationConfig = FlywayMigrationManager.MigrationConfig(),
    val auth: AuthConfig,
    val kafka: KafkaConfig,
    val clients: ClientConfig,
    val arbeidsgiverTiltakCutOffDatoMapping: Map<Avtale.Tiltakstype, LocalDate> = mapOf(
        Avtale.Tiltakstype.SOMMERJOBB to LocalDate.of(2021, 1, 1),
        Avtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD to LocalDate.of(2023, 2, 1),
        Avtale.Tiltakstype.VARIG_LONNSTILSKUDD to LocalDate.of(2023, 2, 1),
        Avtale.Tiltakstype.ARBEIDSTRENING to LocalDate.of(2025, 1, 24),
    ),
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
    val privateJwk: String,
)

data class ServiceClientConfig(
    val url: String,
    val scope: String,
)

data class KafkaConfig(
    val consumerPreset: Properties,
    val consumers: KafkaConsumers,
)

data class KafkaConsumers(
    val amtDeltakerV1: KafkaTopicConsumer.Config,
    val sisteTiltaksgjennomforingerV1: KafkaTopicConsumer.Config,
)
