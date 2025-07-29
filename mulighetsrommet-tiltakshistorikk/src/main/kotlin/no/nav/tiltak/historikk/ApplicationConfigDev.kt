package no.nav.tiltak.historikk

import no.nav.mulighetsrommet.database.DatabaseConfig
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
    kafka = KafkaConfig(
        consumers = KafkaConsumers(),
    ),
    clients = ClientConfig(
        tiltakDatadeling = ServiceClientConfig(
            url = "http://tiltak-datadeling.team-tiltak",
            scope = "api://dev-gcp.team-tiltak.tiltak-datadeling/.default",
        ),
    ),
)
