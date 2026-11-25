package no.nav.tiltak.historikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.maam.dto.KafkaConsumerRecordDto
import no.nav.security.mock.oauth2.MockOAuth2Server

class MaamRoutesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }
    afterSpec {
        oauth.shutdown()
    }
    afterEach { database.truncateAll() }

    test("henting av feilede Kafka-meldinger krever autentisering") {
        withTestApplication(oauth) {
            client.get("/maam/kafka/failed-records").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("henting av feilede Kafka-meldinger krever teamtilgang") {
        withTestApplication(oauth) {
            client.get("/maam/kafka/failed-records") {
                bearerAuth(oauth.issueToken().serialize())
            }.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("henter feilede Kafka-meldinger med teamtilgang") {
        database.db.session {
            it.update(
                queryOf(
                    """
                        insert into kafka_consumer_record
                            (topic, partition, record_offset, retries, key, value, record_timestamp)
                        values ('tiltakshistorikk-test', 0, 1, 1, ?, ?, 123)
                    """.trimIndent(),
                    "key".toByteArray(),
                    """{"test":true}""".toByteArray(),
                ),
            )
        }

        withTestApplication(oauth) {
            val response = client.get("/maam/kafka/failed-records") {
                bearerAuth(
                    oauth.issueToken(
                        claims = mapOf("groups" to listOf(teamMulighetsrommetTestEntraAdGroupId)),
                    ).serialize(),
                )
            }

            response.status shouldBe HttpStatusCode.OK
            val records = response.body<List<KafkaConsumerRecordDto>>()
            records.size shouldBe 1
            records.single().topic shouldBe "tiltakshistorikk-test"
            records.single().retries shouldBe 1
            records.single().key shouldBe "key"
            records.single().value shouldBe """{"test":true}"""
        }
    }
})
