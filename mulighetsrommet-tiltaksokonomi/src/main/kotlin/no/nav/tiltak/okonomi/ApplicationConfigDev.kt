package no.nav.tiltak.okonomi

import io.ktor.client.engine.cio.*
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer

val ApplicationConfigDev = AppConfig(
    httpClientEngine = CIO.create(),
    database = DatabaseConfig(
        jdbcUrl = System.getenv("DB_JDBC_URL"),
        maximumPoolSize = 10,
    ),
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = System.getenv("AZURE_OPENID_CONFIG_ISSUER"),
            jwksUri = System.getenv("AZURE_OPENID_CONFIG_JWKS_URI"),
            audience = System.getenv("AZURE_APP_CLIENT_ID"),
            tokenEndpointUrl = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        ),
    ),
    clients = ClientConfig(
        oebsTiltakApi = AuthenticatedHttpClientConfig(
            url = "https://oebs-valp-api-t1.dev-fss-pub.nais.io",
            scope = "api://dev-fss.team-oebs.oebs-valp-api-t1/.default",
        ),
    ),
    kafka = KafkaConfig(
        consumerPropertiesPreset = KafkaPropertiesPreset.aivenDefaultConsumerProperties("team-mulighetsrommet.tiltaksokonomi.v1"),
        clients = KafkaClients(
            okonomiBestillingConsumer = KafkaTopicConsumer.Config(
                id = "bestilling",
                consumerGroupId = "tiltaksokonomi.bestilling.v1",
                topic = "team-mulighetsrommet.tiltaksokonomi-bestilling-v1",
            ),
        ),
    ),
)
