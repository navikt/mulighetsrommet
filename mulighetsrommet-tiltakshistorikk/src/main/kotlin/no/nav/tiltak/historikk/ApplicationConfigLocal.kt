package no.nav.tiltak.historikk

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.mulighetsrommet.tokenprovider.TokenReponse
import org.apache.kafka.common.serialization.ByteArrayDeserializer

private val consumerProperties = KafkaPropertiesBuilder.consumerBuilder()
    .withBaseProperties()
    .withConsumerGroupId("tiltakshistorikk.v1")
    .withBrokerUrl("localhost:29092")
    .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
    .build()

val ApplicationConfigLocal = AppConfig(
    server = ServerConfig(port = 8070),
    database = DatabaseConfig(
        jdbcUrl = "jdbc:postgresql://localhost:5442/mr-tiltakshistorikk?user=valp&password=valp",
        maximumPoolSize = 10,
    ),
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = "http://localhost:8081/azure",
            jwksUri = "http://localhost:8081/azure/jwks",
            audience = "mr-tiltakshistorikk",
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
    kafka = KafkaConfig(
        consumers = KafkaConsumers(
            amtDeltakerV1 = KafkaTopicConsumer.Config(
                id = "amt-deltaker",
                topic = "amt-deltaker-v1",
                consumerProperties = consumerProperties,
            ),
            sisteTiltaksgjennomforingerV1 = KafkaTopicConsumer.Config(
                id = "siste-tiltaksgjennomforinger",
                topic = "siste-tiltaksgjennomforinger-v1",
                consumerProperties = consumerProperties,
            ),
        ),
    ),
    clients = ClientConfig(
        tiltakDatadeling = ServiceClientConfig(url = "http://localhost:8090/tiltak-datadeling", scope = "default"),
    ),
)
