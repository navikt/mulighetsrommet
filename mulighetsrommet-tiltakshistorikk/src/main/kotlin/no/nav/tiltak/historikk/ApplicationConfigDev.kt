package no.nav.tiltak.historikk

import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer

val ApplicationConfigDev = AppConfig(
    database = DatabaseConfig(
        jdbcUrl = System.getenv("DB_JDBC_URL"),
        maximumPoolSize = 10,
    ),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = System.getenv("AZURE_OPENID_CONFIG_ISSUER"),
            jwksUri = System.getenv("AZURE_OPENID_CONFIG_JWKS_URI"),
            audience = System.getenv("AZURE_APP_CLIENT_ID"),
            tokenEndpointUrl = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            privateJwk = System.getenv("AZURE_APP_JWK"),
        ),
    ),
    kafka = run {
        val defaultConsumerGroupId = "tiltakshistorikk-kafka-consumer.v1"
        KafkaConfig(
            defaultConsumerGroupId = defaultConsumerGroupId,
            consumers = KafkaConsumers(
                amtDeltakerV1 = KafkaTopicConsumer.Config(
                    id = "amt-deltaker",
                    topic = "amt.deltaker-v1",
                ),
                sisteTiltaksgjennomforingerV1 = KafkaTopicConsumer.Config(
                    id = "siste-tiltaksgjennomforinger",
                    topic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
                ),
            ),
            consumerPreset = KafkaPropertiesPreset.aivenDefaultConsumerProperties(defaultConsumerGroupId),
        )
    },
    clients = ClientConfig(
        tiltakDatadeling = ServiceClientConfig(
            url = "http://tiltak-datadeling.team-tiltak",
            scope = "api://dev-gcp.team-tiltak.tiltak-datadeling/.default",
        ),
    ),
)
