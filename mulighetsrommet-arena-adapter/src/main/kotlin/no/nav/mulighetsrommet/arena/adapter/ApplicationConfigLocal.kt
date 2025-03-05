package no.nav.mulighetsrommet.arena.adapter

import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.NotifyFailedEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.tokenprovider.createMockRSAKey
import org.apache.kafka.common.serialization.ByteArrayDeserializer

val ApplicationConfigLocal = Config(
    server = ServerConfig(
        port = 8084,
        host = "0.0.0.0",
    ),
    app = AppConfig(
        enableFailedRecordProcessor = true,
        tasks = TaskConfig(
            retryFailedEvents = RetryFailedEvents.Config(
                delayOfMinutes = 1,
            ),
            notifyFailedEvents = NotifyFailedEvents.Config(
                maxRetries = 5,
                cron = "0 0 7 * * MON-FRI",
            ),
        ),
        services = ServiceConfig(
            mulighetsrommetApi = ServiceClientConfig(
                url = "http://localhost:8080",
                scope = "default",
            ),
            tiltakshistorikk = ServiceClientConfig(
                url = "http://localhost:8080",
                scope = "default",
            ),
            arenaEventService = ArenaEventService.Config(
                channelCapacity = 100,
                numChannelConsumers = 10,
                maxRetries = 5,
            ),
            arenaOrdsProxy = ServiceClientConfig(
                url = "http://localhost:8090/arena-ords-proxy",
                scope = "default",
            ),
        ),
        database = DatabaseConfig(
            jdbcUrl = "jdbc:postgresql://localhost:5442/mr-arena-adapter?user=valp&password=valp",
            maximumPoolSize = 10,
        ),
        flyway = FlywayMigrationManager.MigrationConfig(),
        kafka = KafkaConfig(
            consumerPreset = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId("mulighetsrommet-kafka-consumer.v1")
                .withBrokerUrl("localhost:29092")
                .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
                .build(),
            consumers = KafkaConsumers(
                arenaTiltakEndret = KafkaTopicConsumer.Config(
                    id = "arena-tiltakstype-endret",
                    topic = "tiltak-endret",
                ),
                arenaTiltakgjennomforingEndret = KafkaTopicConsumer.Config(
                    id = "arena-tiltakgjennomforing-endret",
                    topic = "tiltakgjennomforingendret",
                ),
                arenaTiltakdeltakerEndret = KafkaTopicConsumer.Config(
                    id = "arena-tiltakdeltaker-endret",
                    topic = "tiltakdeltakerendret",
                ),
                arenaHistTiltakdeltakerEndret = KafkaTopicConsumer.Config(
                    id = "arena-hist-tiltakdeltaker-endret",
                    topic = "histtiltakdeltakerendret",
                ),
                arenaSakEndret = KafkaTopicConsumer.Config(
                    id = "arena-sakendret-endret",
                    topic = "sakendret",
                ),
                arenaAvtaleInfoEndret = KafkaTopicConsumer.Config(
                    id = "arena-avtaleinfo-endret",
                    topic = "avtaleinfo-endret",
                ),
            ),
        ),
        auth = AuthConfig(
            azure = AuthProvider(
                issuer = "http://localhost:8081/azure",
                jwksUri = "http://localhost:8081/azure/jwks",
                audience = "mulighetsrommet-api",
                tokenEndpointUrl = "http://localhost:8081/azure/token",
                privateJwk = createMockRSAKey("azure"),
            ),
        ),
        slack = SlackConfig(
            token = "SLACK_TOKEN",
            channel = "#team-valp-monitoring",
            enable = false,
        ),
    ),
)
