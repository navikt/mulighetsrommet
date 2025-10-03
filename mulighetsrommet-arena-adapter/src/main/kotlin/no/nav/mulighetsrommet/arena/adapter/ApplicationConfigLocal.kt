package no.nav.mulighetsrommet.arena.adapter

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.NotifyFailedEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.mulighetsrommet.tokenprovider.TokenReponse
import org.apache.kafka.common.serialization.ByteArrayDeserializer

private val arenaAdapterConsumerProperties = KafkaPropertiesBuilder.consumerBuilder()
    .withBaseProperties()
    .withConsumerGroupId("mulighetsrommet-kafka-consumer.v1")
    .withBrokerUrl("localhost:29092")
    .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
    .build()

val ApplicationConfigLocal = AppConfig(
    server = ServerConfig(
        port = 8084,
        host = "0.0.0.0",
    ),
    migrering = MigreringConfig(),
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
        consumers = KafkaConsumers(
            arenaTiltakEndret = KafkaTopicConsumer.Config(
                id = "arena-tiltakstype-endret",
                topic = "tiltak-endret",
                consumerProperties = arenaAdapterConsumerProperties,
            ),
            arenaTiltakgjennomforingEndret = KafkaTopicConsumer.Config(
                id = "arena-tiltakgjennomforing-endret",
                topic = "tiltakgjennomforingendret",
                consumerProperties = arenaAdapterConsumerProperties,
            ),
            arenaTiltakdeltakerEndret = KafkaTopicConsumer.Config(
                id = "arena-tiltakdeltaker-endret",
                topic = "tiltakdeltakerendret",
                consumerProperties = arenaAdapterConsumerProperties,
            ),
            arenaHistTiltakdeltakerEndret = KafkaTopicConsumer.Config(
                id = "arena-hist-tiltakdeltaker-endret",
                topic = "histtiltakdeltakerendret",
                consumerProperties = arenaAdapterConsumerProperties,
            ),
            arenaSakEndret = KafkaTopicConsumer.Config(
                id = "arena-sakendret-endret",
                topic = "sakendret",
                consumerProperties = arenaAdapterConsumerProperties,
            ),
            arenaAvtaleInfoEndret = KafkaTopicConsumer.Config(
                id = "arena-avtaleinfo-endret",
                topic = "avtaleinfo-endret",
                consumerProperties = arenaAdapterConsumerProperties,
            ),
        ),
    ),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = "http://localhost:8081/azure",
            jwksUri = "http://localhost:8081/azure/jwks",
            audience = "mulighetsrommet-api",
            tokenEndpointUrl = "http://localhost:8081/azure/token",
            privateJwk = "azure",
        ),
        texas = TexasClient.Config(
            tokenEndpoint = "http://localhost:8090/api/v1/token",
            tokenExchangeEndpoint = "http://localhost:8090/api/v1/token/exchange",
            tokenIntrospectionEndpoint = "http://localhost:8090/api/v1/introspect",
            engine = MockEngine { _ ->
                respond(
                    content = Json.encodeToString(
                        TokenReponse(
                            access_token = "dummy",
                            token_type = TokenReponse.TokenType.Bearer,
                            expires_in = 1_000_1000,
                        ),
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ),
    ),
    slack = SlackConfig(
        token = "SLACK_TOKEN",
        channel = "#team-valp-monitoring",
        enable = false,
    ),
)
