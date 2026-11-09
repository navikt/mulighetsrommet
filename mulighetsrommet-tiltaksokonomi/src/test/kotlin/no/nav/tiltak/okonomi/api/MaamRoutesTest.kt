package no.nav.tiltak.okonomi.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.okonomi.databaseConfig
import no.nav.tiltak.okonomi.plugins.AppRoles
import no.nav.tiltak.okonomi.withTestApplication

class MaamRoutesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val oauth = MockOAuth2Server()

    beforeSpec { oauth.start() }
    afterSpec { oauth.shutdown() }

    test("sletting av feilet Kafka-melding krever autentisering og er sperret i produksjon") {
        val id = database.db.session { session ->
            session.single(
                queryOf(
                    """
                        insert into kafka_consumer_record (topic, partition, record_offset, retries, record_timestamp)
                        values ('maam-test', 0, 1, 1, 0)
                        returning id
                    """.trimIndent(),
                ),
            ) { it.long("id") }!!
        }
        val token = oauth.issueToken(
            claims = mapOf("roles" to listOf(AppRoles.ACCESS_AS_APPLICATION)),
        ).serialize()

        withTestApplication(oauth) {
            client.delete("/maam/topics/failed-records") {
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(id, "maam-test"))
            }.status shouldBe HttpStatusCode.Unauthorized

            mockkObject(NaisEnv.Companion)
            try {
                every { NaisEnv.current() } returns NaisEnv.ProdGCP
                client.delete("/maam/topics/failed-records") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(DeleteKafkaRecordRequest(id, "maam-test"))
                }.status shouldBe HttpStatusCode.Forbidden
            } finally {
                unmockkObject(NaisEnv.Companion)
            }

            database.db.session { session ->
                session.single(queryOf("select id from kafka_consumer_record where id = ?", id)) { it.long("id") }
            } shouldBe id

            client.delete("/maam/topics/failed-records") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(id, "maam-test"))
            }.status shouldBe HttpStatusCode.NoContent

            database.db.session { session ->
                session.single(queryOf("select id from kafka_consumer_record where id = ?", id)) { it.long("id") }
            } shouldBe null
        }
    }
})
