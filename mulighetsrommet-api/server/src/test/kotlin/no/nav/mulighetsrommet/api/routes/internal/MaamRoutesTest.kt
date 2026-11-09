package no.nav.mulighetsrommet.api.routes.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.navansatt.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

class MaamRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val ansatt = NavAnsattFixture.DonaldDuck
    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(ansatt),
    )

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.api)
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val teamMulighetsrommetRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TEAM_MULIGHETSROMMET)

    fun appConfig() = ApplicationConfigTest.copy(
        auth = createAuthConfig(
            oauth,
            roles = setOf(generellRolle, teamMulighetsrommetRolle),
        ),
    )

    test("sletter bare den valgte feilede Kafka-meldingen") {
        val id = database.api.insertKafkaRecord(1, retries = 1)
        val otherId = database.api.insertKafkaRecord(2, retries = 1)
        val pendingId = database.api.insertKafkaRecord(3, retries = 0)
        val token = oauth.issueToken(claims = getAnsattClaims(ansatt, setOf(teamMulighetsrommetRolle))).serialize()

        withTestApplication(appConfig()) {
            val wrongTopic = client.delete("/api/intern/maam/topics/failed-records") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(id, "other-topic"))
            }
            wrongTopic.status shouldBe HttpStatusCode.Conflict
            database.api.kafkaRecordExists(id) shouldBe true

            val pending = client.delete("/api/intern/maam/topics/failed-records") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(pendingId, "maam-test"))
            }
            pending.status shouldBe HttpStatusCode.Conflict
            database.api.kafkaRecordExists(pendingId) shouldBe true

            val response = client.delete("/api/intern/maam/topics/failed-records") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(id, "maam-test"))
            }
            response.status shouldBe HttpStatusCode.NoContent
            database.api.kafkaRecordExists(id) shouldBe false
            database.api.kafkaRecordExists(otherId) shouldBe true
        }
    }

    test("sletter bare valgt task og beholder aktive og ikke-feilede tasks") {
        database.api.insertTask("selected", "1")
        database.api.insertTask("selected", "2")
        database.api.insertTask("other", "1")
        database.api.insertTask("running", "1", picked = true)
        database.api.insertTask("pending", "1", failures = 0)
        val token = oauth.issueToken(claims = getAnsattClaims(ansatt, setOf(teamMulighetsrommetRolle))).serialize()

        withTestApplication(appConfig()) {
            forAll(
                row("running", HttpStatusCode.Conflict),
                row("pending", HttpStatusCode.Conflict),
                row("selected", HttpStatusCode.NoContent),
            ) { name, status ->
                val response = client.delete("/api/intern/maam/tasks/scheduled") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(DeleteScheduledTaskRequest(name, "1"))
                }
                response.status shouldBe status
            }

            database.api.taskExists("selected", "1") shouldBe false
            database.api.taskExists("selected", "2") shouldBe true
            database.api.taskExists("other", "1") shouldBe true
            database.api.taskExists("running", "1") shouldBe true
            database.api.taskExists("pending", "1") shouldBe true
        }
    }

    test("sletting er ikke tilgjengelig uten teamrollen") {
        val id = database.api.insertKafkaRecord(4, retries = 1)
        database.api.insertTask("protected", "1")
        val endpoints = listOf(
            "/api/intern/maam/topics/failed-records" to """{"id":$id,"topic":"maam-test"}""",
            "/api/intern/maam/tasks/scheduled" to """{"taskName":"protected","taskInstance":"1"}""",
        )

        withTestApplication(appConfig()) {
            val generalToken = oauth.issueToken(claims = getAnsattClaims(ansatt, setOf(generellRolle))).serialize()
            endpoints.forEach { (path, body) ->
                client.delete(path) {
                    bearerAuth(generalToken)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.status shouldBe HttpStatusCode.Forbidden
            }

            database.api.kafkaRecordExists(id) shouldBe true
            database.api.taskExists("protected", "1") shouldBe true
        }
    }

    test("sletting er ikke tilgjengelig i produksjon") {
        val id = database.api.insertKafkaRecord(5, retries = 1)
        database.api.insertTask("protected", "2")
        val endpoints = listOf(
            "/api/intern/maam/topics/failed-records" to """{"id":$id,"topic":"maam-test"}""",
            "/api/intern/maam/tasks/scheduled" to """{"taskName":"protected","taskInstance":"2"}""",
        )

        withTestApplication(appConfig()) {
            startApplication()
            mockkObject(NaisEnv.Companion)
            try {
                every { NaisEnv.current() } returns NaisEnv.ProdGCP
                val teamToken = oauth.issueToken(
                    claims = getAnsattClaims(ansatt, setOf(teamMulighetsrommetRolle)),
                ).serialize()
                endpoints.forEach { (path, body) ->
                    client.delete(path) {
                        bearerAuth(teamToken)
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }.status shouldBe HttpStatusCode.Forbidden
                }
            } finally {
                unmockkObject(NaisEnv.Companion)
            }
            database.api.kafkaRecordExists(id) shouldBe true
            database.api.taskExists("protected", "2") shouldBe true
        }
    }

    test("maam er bare tilgjengelig med når man har rollen TEAM_MULIGHETSROMMET") {
        withTestApplication(appConfig()) {
            forAll(
                row(generellRolle, HttpStatusCode.Forbidden),
                row(teamMulighetsrommetRolle, HttpStatusCode.OK),
            ) { rolle, status ->
                val claims = getAnsattClaims(ansatt, setOf(rolle))

                val response = client.get("/api/intern/maam/topics") {
                    bearerAuth(oauth.issueToken(claims = claims).serialize())
                    contentType(ContentType.Application.Json)
                }

                response.status shouldBe status
            }
        }
    }
})

private fun ApiDatabase.insertKafkaRecord(offset: Long, retries: Int): Long = session {
    session.single(
        queryOf(
            """
                insert into kafka_consumer_record (topic, partition, record_offset, retries, record_timestamp)
                values ('maam-test', 0, ?, ?, 0)
                returning id
            """.trimIndent(),
            offset,
            retries,
        ),
    ) { it.long("id") }!!
}

private fun ApiDatabase.kafkaRecordExists(id: Long): Boolean = session {
    session.single(queryOf("select id from kafka_consumer_record where id = ?", id)) { it.long("id") } != null
}

private fun ApiDatabase.insertTask(name: String, instance: String, failures: Int = 1, picked: Boolean = false) = session {
    session.update(
        queryOf(
            """
                insert into scheduled_tasks
                    (task_name, task_instance, execution_time, picked, consecutive_failures, version)
                values (?, ?, current_timestamp, ?, ?, 1)
            """.trimIndent(),
            name,
            instance,
            picked,
            failures,
        ),
    )
}

private fun ApiDatabase.taskExists(name: String, instance: String): Boolean = session {
    session.single(
        queryOf(
            "select task_name from scheduled_tasks where task_name = ? and task_instance = ?",
            name,
            instance,
        ),
    ) { it.string("task_name") } != null
}
