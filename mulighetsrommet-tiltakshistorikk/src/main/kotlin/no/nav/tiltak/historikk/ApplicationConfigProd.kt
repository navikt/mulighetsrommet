package no.nav.tiltak.historikk

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.metrics.Metrics
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.tiltak.historikk.model.ArrangorDeltakerHistorikkKilde
import java.util.UUID

val ApplicationConfigProd = AppConfig(
    database = DatabaseConfig(
        jdbcUrl = System.getenv("DB_JDBC_URL"),
        maximumPoolSize = 20,
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
        teamMulighetsrommetEntraAdGroupId = UUID.fromString("debefa6e-1865-446d-b22b-9579fc735de3"),
    ),
    kafka = KafkaConfig(
        consumers = KafkaConsumers(),
    ),
    arrangorTiltakHistorikkKilder = mapOf(
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to ArrangorDeltakerHistorikkKilde.ARENA,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING to ArrangorDeltakerHistorikkKilde.ARENA,
        Tiltakskode.HOYERE_UTDANNING to ArrangorDeltakerHistorikkKilde.ARENA,
    ),
)
