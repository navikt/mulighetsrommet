package no.nav.tiltak.okonomi

import io.ktor.client.engine.mock.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import org.intellij.lang.annotations.Language

val mockClientEngine = createMockEngine {
    get("https://data.brreg.no/enhetsregisteret/api/enheter/\\d+".toRegex()) {
        @Language("json")
        val brregResponse = """
            {
                "organisasjonsnummer": "123456789",
                "navn": "Tiltaksarrangør AS",
                "organisasjonsform": {
                    "kode": "AS",
                    "beskrivelse": "Aksjeselskap"
                },
                "postadresse": {
                    "land": "Norge",
                    "landkode": "NO",
                    "postnummer": "0170",
                    "poststed": "OSLO",
                    "adresse": ["Gateveien 1"],
                    "kommune": "OSLO",
                    "kommunenummer": "0301"
                }
            }
        """.trimIndent()
        respondJson(brregResponse)
    }

    post("http://oebs-tiltak-api/api/v1/tilsagn") { respondOk() }

    post("http://oebs-tiltak-api/api/v1/refusjonskrav") { respondOk() }
}

val ApplicationConfigLocal = AppConfig(
    server = ServerConfig(port = 8074),
    httpClientEngine = mockClientEngine,
    database = DatabaseConfig(
        jdbcUrl = "jdbc:postgresql://localhost:5442/mr-tiltaksokonomi?user=valp&password=valp",
        maximumPoolSize = 10,
    ),
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = "http://localhost:8081/azure",
            jwksUri = "http://localhost:8081/azure/jwks",
            audience = "mr-tiltaksokonomi",
            tokenEndpointUrl = "http://localhost:8081/azure/token",
        ),
    ),
    clients = ClientConfig(
        oebsTiltakApi = AuthenticatedHttpClientConfig(url = "http://oebs-tiltak-api", scope = "default"),
    ),
    kafka = KafkaConfig(
        brokerUrl = "localhost:29092",
        defaultConsumerGroupId = "tiltaksokonomi.v1",
        clients = KafkaClients(
            okonomiBestillingConsumer = KafkaTopicConsumer.Config(
                id = "bestilling",
                topic = "tiltaksokonomi-bestilling-v1",
            ),
        ),
    ),
)
