package no.nav.tiltak.historikk

import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.tokenprovider.createMockRSAKey
import org.apache.kafka.common.serialization.ByteArrayDeserializer

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
            privateJwk = createMockRSAKey("azure"),
        ),
    ),
    kafka = run {
        val brokerUrl = "localhost:29092"
        val defaultConsumerGroupId = "tiltakshistorikk.v1"
        KafkaConfig(
            brokerUrl = brokerUrl,
            defaultConsumerGroupId = defaultConsumerGroupId,
            consumers = KafkaConsumers(
                amtDeltakerV1 = KafkaTopicConsumer.Config(
                    id = "amt-deltaker",
                    topic = "amt-deltaker-v1",
                ),
                sisteTiltaksgjennomforingerV1 = KafkaTopicConsumer.Config(
                    id = "siste-tiltaksgjennomforinger",
                    topic = "siste-tiltaksgjennomforinger-v1",
                ),
            ),
            consumerPreset = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(defaultConsumerGroupId)
                .withBrokerUrl(brokerUrl)
                .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
                .build(),

        )
    },
    clients = ClientConfig(
        tiltakDatadeling = ServiceClientConfig(url = "http://localhost:8090/tiltak-datadeling", scope = "default"),
    ),
)
