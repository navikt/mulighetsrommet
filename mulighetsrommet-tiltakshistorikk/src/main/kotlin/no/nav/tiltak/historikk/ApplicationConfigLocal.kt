package no.nav.tiltak.historikk

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.database.DatabaseConfig
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
            replikerSisteTiltakstyper = KafkaTopicConsumer.Config(
                id = "repliker-siste-tiltakstyper",
                topic = "siste-tiltakstyper-v3",
                consumerProperties = consumerProperties,
            ),
            replikerSisteTiltaksgjennomforinger = KafkaTopicConsumer.Config(
                id = "repliker-tiltaksgjennomforinger",
                topic = "siste-tiltaksgjennomforinger-v2",
                consumerProperties = consumerProperties,
            ),
            replikerAmtDeltaker = KafkaTopicConsumer.Config(
                id = "repliker-amt-deltaker",
                topic = "amt-deltaker-v1",
                consumerProperties = consumerProperties,
            ),
            replikerAmtVirksomhet = KafkaTopicConsumer.Config(
                id = "repliker-amt-virksomheter",
                topic = "amt.virksomheter-v1",
                consumerProperties = consumerProperties,
            ),
        ),
    ),
    clients = ClientConfig(
        tiltakDatadeling = ServiceClientConfig(url = "http://localhost:8090/tiltak-datadeling", scope = "default"),
    ),
)
