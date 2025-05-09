package no.nav.tiltak.okonomi

import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer

val ApplicationConfigProd = AppConfig(
    database = DatabaseConfig(
        jdbcUrl = System.getenv("DB_JDBC_URL"),
        maximumPoolSize = 20,
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
    clients = ClientConfig(
        oebsPoAp = AuthenticatedHttpClientConfig(
            url = "https://oebs-po-ap-api.prod-fss-pub.nais.io",
            scope = "api://prod-fss.team-oebs.oebs-po-ap-api/.default",
        ),
    ),
    kafka = KafkaConfig(
        consumerPropertiesPreset = KafkaPropertiesPreset.aivenDefaultConsumerProperties("team-mulighetsrommet.tiltaksokonomi.v1"),
        producerPropertiesPreset = KafkaPropertiesPreset.aivenByteProducerProperties("team-mulighetsrommet.tiltaksokonomi.v1"),
        topics = KafkaTopics(
            bestillingStatus = "team-mulighetsrommet.tiltaksokonomi.bestilling-status-v1",
            fakturaStatus = "team-mulighetsrommet.tiltaksokonomi.faktura-status-v1",
        ),
        clients = KafkaClients(
            okonomiBestillingConsumer = KafkaTopicConsumer.Config(
                id = "bestilling",
                consumerGroupId = "tiltaksokonomi.bestilling.v1",
                topic = "team-mulighetsrommet.tiltaksokonomi.bestillinger-v1",
            ),
        ),
    ),
)
