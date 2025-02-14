package no.nav.tiltak.historikk

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig

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
        ),
    ),
    kafka = KafkaConfig(
        brokerUrl = "localhost:29092",
        defaultConsumerGroupId = "tiltakshistorikk.v1",
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
    ),
    clients = ClientConfig(
        tiltakDatadeling = ServiceClientConfig(url = "http://localhost:8090/tiltak-datadeling", scope = "default"),
    ),
)
