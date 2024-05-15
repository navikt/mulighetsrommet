package no.nav.mulighetsrommet.arena.adapter

import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.NotifyFailedEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig,
)

data class AppConfig(
    val enableFailedRecordProcessor: Boolean,
    val tasks: TaskConfig,
    val services: ServiceConfig,
    val database: DatabaseConfig,
    val flyway: FlywayMigrationManager.MigrationConfig = FlywayMigrationManager.MigrationConfig(),
    val kafka: KafkaConfig,
    val auth: AuthConfig,
    val slack: SlackConfig,
)

data class TaskConfig(
    val retryFailedEvents: RetryFailedEvents.Config,
    val notifyFailedEvents: NotifyFailedEvents.Config,
)

data class ServiceConfig(
    val mulighetsrommetApi: ServiceClientConfig,
    val arenaEventService: ArenaEventService.Config,
    val arenaOrdsProxy: ServiceClientConfig,
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
    val arenaTiltakEndret: KafkaTopicConsumer.Config,
    val arenaTiltakgjennomforingEndret: KafkaTopicConsumer.Config,
    val arenaTiltakdeltakerEndret: KafkaTopicConsumer.Config,
    val arenaSakEndret: KafkaTopicConsumer.Config,
    val arenaAvtaleInfoEndret: KafkaTopicConsumer.Config,
)

data class SlackConfig(
    val token: String,
    val channel: String,
    val enable: Boolean,
)
