package no.nav.tiltak.okonomi

import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.metrics.Metrics
import no.nav.mulighetsrommet.tokenprovider.TexasClient

val ApplicationConfigDev = AppConfig(
    database = DatabaseConfig(
        jdbcUrl = System.getenv("DB_JDBC_URL"),
        maximumPoolSize = 10,
        micrometerRegistry = Metrics.micrometerRegistry,
    ),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = System.getenv("AZURE_OPENID_CONFIG_ISSUER"),
            jwksUri = System.getenv("AZURE_OPENID_CONFIG_JWKS_URI"),
            audience = System.getenv("AZURE_APP_CLIENT_ID"),
            tokenEndpointUrl = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            privateJwk = System.getenv("AZURE_APP_JWK"),
        ),
        texas = TexasClient.Config(
            tokenEndpoint = System.getenv("NAIS_TOKEN_ENDPOINT"),
            tokenExchangeEndpoint = System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
            tokenIntrospectionEndpoint = System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT"),
        ),
    ),
    clients = ClientConfig(
        oebsPoAp = AuthenticatedHttpClientConfig(
            url = "https://oebs-po-ap-api-q1.dev-fss-pub.nais.io",
            scope = "api://dev-fss.team-oebs.oebs-po-ap-api-q1/.default",
        ),
    ),
    slack = SlackConfig(
        token = System.getenv("SLACK_TOKEN"),
        channel = "#team-valp-monitorering-dev",
        enable = true,
    ),
    kafka = KafkaConfig(
        producerPropertiesPreset = KafkaPropertiesPreset.aivenByteProducerProperties("team-mulighetsrommet.tiltaksokonomi.v1"),
        topics = KafkaTopics(
            bestillingStatus = "team-mulighetsrommet.tiltaksokonomi.bestilling-status-v1",
            fakturaStatus = "team-mulighetsrommet.tiltaksokonomi.faktura-status-v1",
            utbetaling = "team-mulighetsrommet.tiltaksokonomi.utbetaling-v1",
        ),
        clients = KafkaClients(
            okonomiBestillingConsumer = KafkaTopicConsumer.Config(
                id = "bestilling",
                topic = "team-mulighetsrommet.tiltaksokonomi.bestillinger-v1",
                consumerProperties = KafkaPropertiesPreset.aivenDefaultConsumerProperties("tiltaksokonomi.bestilling.v1"),
            ),
            okonomiBestillingBrukerConsumer = KafkaTopicConsumer.Config(
                id = "bestilling-bruker",
                topic = "team-mulighetsrommet.tiltaksokonomi.bestillinger-bruker-v1",
                consumerProperties = KafkaPropertiesPreset.aivenDefaultConsumerProperties("tiltaksokonomi.bestillinger-bruker-v1"),
            ),
            helvedStatusConsumer = KafkaTopicConsumer.Config(
                id = "helved-status",
                topic = "helved.status.v1",
                consumerProperties = KafkaPropertiesPreset.aivenDefaultConsumerProperties("helved.status.v1"),
            ),
        ),
    ),
    faktura = FakturaConfig(
        tidligstTidspunktForUtbetaling = tidligstTidspunktForUtbetalingDev,
    ),
)
