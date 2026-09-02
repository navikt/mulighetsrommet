package no.nav.tiltak.historikk

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.metrics.Metrics
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import java.time.LocalDate
import java.util.UUID

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
        teamMulighetsrommetEntraAdGroupId = UUID.fromString("639e2806-4cc2-484c-a72a-51b4308c52a1"),
    ),
    kafka = KafkaConfig(
        consumers = KafkaConsumers(),
    ),
    arbeidsgiverTiltakCutOffDatoMapping = mapOf(
        Tiltakskode.SOMMERJOBB to LocalDate.of(2021, 1, 1),
        Tiltakskode.MIDLERTIDIG_LONNSTILSKUDD to LocalDate.of(2023, 2, 1),
        Tiltakskode.VARIG_LONNSTILSKUDD to LocalDate.of(2023, 2, 1),
        Tiltakskode.ARBEIDSTRENING to LocalDate.of(2025, 1, 24),
        Tiltakskode.VTAO to LocalDate.of(2025, 1, 1),
        Tiltakskode.MENTOR to LocalDate.of(2025, 1, 1),
    ),
)
