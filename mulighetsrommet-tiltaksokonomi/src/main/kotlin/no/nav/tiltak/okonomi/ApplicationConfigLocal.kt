package no.nav.tiltak.okonomi

import io.ktor.client.engine.mock.*
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.ServerConfig
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.tokenprovider.createMockRSAKey
import no.nav.tiltak.okonomi.avstemming.SftpClient
import no.nav.tiltak.okonomi.avstemming.task.DailyAvstemming
import no.nav.tiltak.okonomi.oebs.OebsPoApClient
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
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

    post(OebsPoApClient.BESTILLING_ENDPOINT) { respondOk() }

    post(OebsPoApClient.FAKTURA_ENDPOINT) { respondOk() }
}

val ApplicationConfigLocal = AppConfig(
    httpClientEngine = mockClientEngine,
    server = ServerConfig(port = 8074),
    flyway = FlywayMigrationManager.MigrationConfig(),
    database = DatabaseConfig(
        jdbcUrl = "jdbc:postgresql://localhost:5442/mr-tiltaksokonomi?user=valp&password=valp",
        maximumPoolSize = 10,
    ),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = "http://localhost:8081/azure",
            jwksUri = "http://localhost:8081/azure/jwks",
            audience = "mr-tiltaksokonomi",
            tokenEndpointUrl = "http://localhost:8081/azure/token",
            privateJwk = createMockRSAKey("azure"),
        ),
    ),
    clients = ClientConfig(
        oebsPoAp = AuthenticatedHttpClientConfig(url = "http://localhost", scope = "default"),
    ),
    avstemming = AvstemmingConfig(
        sftpProperties = SftpClient.SftpProperties(
            username = "todo",
            host = "todo",
            port = 8080,
            privateKey = "todo",
            directory = "todo",
        ),
        dailyTask = DailyAvstemming.Config(
            disabled = true,
        ),
    ),
    slack = SlackConfig(
        token = System.getenv("SLACK_TOKEN") ?: "",
        channel = "#team-valp-monitoring",
        enable = false,
    ),
    kafka = KafkaConfig(
        producerPropertiesPreset = KafkaPropertiesBuilder.producerBuilder()
            .withBaseProperties()
            .withProducerId("tiltaksokonomi.v1")
            .withBrokerUrl("localhost:29092")
            .withSerializers(ByteArraySerializer::class.java, ByteArraySerializer::class.java)
            .build(),
        topics = KafkaTopics(
            bestillingStatus = "tiltaksokonomi.bestilling-status-v1",
            fakturaStatus = "tiltaksokonomi.faktura-status-v1",
        ),
        clients = KafkaClients(
            okonomiBestillingConsumer = KafkaTopicConsumer.Config(
                id = "bestilling",
                topic = "tiltaksokonomi.bestillinger-v1",
                consumerProperties = KafkaPropertiesBuilder.consumerBuilder()
                    .withBaseProperties()
                    .withConsumerGroupId("tiltaksokonomi.v1")
                    .withBrokerUrl("localhost:29092")
                    .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
                    .build(),
            ),
        ),
    ),
)
