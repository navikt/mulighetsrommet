package no.nav.mulighetsrommet.maam

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.queries.KafkaConsumerRecordQueries
import no.nav.mulighetsrommet.database.queries.ScheduledTaskQueries
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.maam.dto.DeleteKafkaRecordRequest
import no.nav.mulighetsrommet.maam.dto.DeleteScheduledTaskRequest
import no.nav.mulighetsrommet.maam.dto.KafkaConsumerRecordDto
import no.nav.mulighetsrommet.maam.dto.RetryKafkaRecordRequest
import no.nav.mulighetsrommet.maam.dto.RetryScheduledTaskRequest
import no.nav.mulighetsrommet.maam.dto.ScheduledTaskDto
import java.time.Instant
import java.time.LocalDateTime
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class MaamRoutesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(testDatabaseConfig))

    beforeEach {
        database.truncateAll()
    }

    test("henter bare feilede Kafka-meldinger med dekodet innhold") {
        val id = database.db.insertKafkaRecord(offset = 1)
        database.db.insertKafkaRecord(offset = 2, retries = 0)

        withMaamApplication(database.db) {
            val records = client.get("/maam/kafka/failed-records").body<List<KafkaConsumerRecordDto>>()
            records.map { it.id } shouldBe listOf(id)
            records.single().key shouldBe "key"
            records.single().value shouldBe "value"
            records.single().recordTimestamp shouldBe "123"
        }
    }

    test("endrer tidspunkt for retry av Kafka-melding") {
        val id = database.db.insertKafkaRecord(offset = 1)

        withMaamApplication(database.db) {
            client.put("/maam/kafka/failed-records") {
                contentType(ContentType.Application.Json)
                setBody(RetryKafkaRecordRequest(id, "maam-test", LocalDateTime.parse("2026-01-02T03:04:05")))
            }.status shouldBe HttpStatusCode.OK
        }

        database.db.session { KafkaConsumerRecordQueries(it).getFailedRecords().single().lastRetry } shouldBe
            Instant.parse("2026-01-02T03:04:05Z")
    }

    test("sletter bare valgt Kafka-melding") {
        val id = database.db.insertKafkaRecord(offset = 1)
        val otherId = database.db.insertKafkaRecord(offset = 2)

        withMaamApplication(database.db) {
            client.delete("/maam/kafka/failed-records") {
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(id, "maam-test"))
            }.status shouldBe HttpStatusCode.NoContent
        }

        database.db.kafkaRecordExists(id) shouldBe false
        database.db.kafkaRecordExists(otherId) shouldBe true
    }

    test("sletter ikke Kafka-melding fra et annet topic") {
        val id = database.db.insertKafkaRecord(offset = 1)

        withMaamApplication(database.db) {
            client.delete("/maam/kafka/failed-records") {
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(id, "other-topic"))
            }.status shouldBe HttpStatusCode.Conflict
        }

        database.db.kafkaRecordExists(id) shouldBe true
    }

    test("sletter ikke Kafka-melding som ikke har feilet") {
        val id = database.db.insertKafkaRecord(offset = 1, retries = 0)

        withMaamApplication(database.db) {
            client.delete("/maam/kafka/failed-records") {
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(id, "maam-test"))
            }.status shouldBe HttpStatusCode.Conflict
        }

        database.db.kafkaRecordExists(id) shouldBe true
    }

    test("sletting av Kafka-melding er sperret i produksjon") {
        val id = database.db.insertKafkaRecord(offset = 1)

        withMaamApplication(database.db, environment = NaisEnv.ProdGCP) {
            client.delete("/maam/kafka/failed-records") {
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(id, "maam-test"))
            }.status shouldBe HttpStatusCode.Forbidden
        }

        database.db.kafkaRecordExists(id) shouldBe true
    }

    test("henter bare feilede tasks med dekodet data") {
        database.db.insertTask("failed", "1")
        database.db.insertTask("pending", "1", failures = 0)

        withMaamApplication(database.db) {
            val tasks = client.get("/maam/tasks/scheduled/failed").body<List<ScheduledTaskDto>>()
            tasks.map { it.taskName } shouldBe listOf("failed")
            tasks.single().taskData shouldBe """{"id":"test"}"""
        }
    }

    test("endrer tidspunkt for retry av task") {
        database.db.insertTask("failed", "1")

        withMaamApplication(database.db) {
            client.post("/maam/tasks/scheduled/retry") {
                contentType(ContentType.Application.Json)
                setBody(RetryScheduledTaskRequest("failed", "1", LocalDateTime.parse("2026-01-02T03:04:05")))
            }.status shouldBe HttpStatusCode.OK
        }

        database.db.session { ScheduledTaskQueries(it).getFailedTasks().single().executionTime } shouldBe
            Instant.parse("2026-01-02T03:04:05Z")
    }

    test("sletter bare valgt task med riktig navn og instans") {
        database.db.insertTask("selected", "1")
        database.db.insertTask("selected", "2")
        database.db.insertTask("other", "1")

        withMaamApplication(database.db) {
            client.delete("/maam/tasks/scheduled") {
                contentType(ContentType.Application.Json)
                setBody(DeleteScheduledTaskRequest("selected", "1"))
            }.status shouldBe HttpStatusCode.NoContent
        }

        database.db.taskExists("selected", "1") shouldBe false
        database.db.taskExists("selected", "2") shouldBe true
        database.db.taskExists("other", "1") shouldBe true
    }

    test("sletter ikke task som er plukket av en worker") {
        database.db.insertTask("running", "1", picked = true)

        withMaamApplication(database.db) {
            client.delete("/maam/tasks/scheduled") {
                contentType(ContentType.Application.Json)
                setBody(DeleteScheduledTaskRequest("running", "1"))
            }.status shouldBe HttpStatusCode.Conflict
        }

        database.db.taskExists("running", "1") shouldBe true
    }

    test("sletter ikke task som ikke har feilet") {
        database.db.insertTask("pending", "1", failures = 0)

        withMaamApplication(database.db) {
            client.delete("/maam/tasks/scheduled") {
                contentType(ContentType.Application.Json)
                setBody(DeleteScheduledTaskRequest("pending", "1"))
            }.status shouldBe HttpStatusCode.Conflict
        }

        database.db.taskExists("pending", "1") shouldBe true
    }

    test("sletting av task er sperret i produksjon") {
        database.db.insertTask("failed", "1")

        withMaamApplication(database.db, environment = NaisEnv.ProdGCP) {
            client.delete("/maam/tasks/scheduled") {
                contentType(ContentType.Application.Json)
                setBody(DeleteScheduledTaskRequest("failed", "1"))
            }.status shouldBe HttpStatusCode.Forbidden
        }

        database.db.taskExists("failed", "1") shouldBe true
    }
})

private fun withMaamApplication(
    db: Database,
    environment: NaisEnv = NaisEnv.DevGCP,
    test: suspend ApplicationTestBuilder.() -> Unit,
) = testApplication {
    application {
        install(ContentNegotiation) { json() }
        routing {
            route("/maam") {
                install(Maam) {
                    database = { db }
                    this.environment = environment
                    kafka(orchestrator = { mockk() }) {
                        failedRecords()
                    }
                    tasks {
                        scheduled()
                    }
                }
            }
        }
    }
    client = createClient {
        install(ClientContentNegotiation) { json() }
    }
    test()
}

private fun Database.insertKafkaRecord(offset: Long, retries: Int = 1): Long = session {
    it.single(
        queryOf(
            """
                insert into kafka_consumer_record
                    (topic, partition, record_offset, retries, key, value, record_timestamp)
                values ('maam-test', 0, ?, ?, ?, ?, 123)
                returning id
            """.trimIndent(),
            offset,
            retries,
            "key".toByteArray(),
            "value".toByteArray(),
        ),
    ) { row -> row.long("id") }!!
}

private fun Database.kafkaRecordExists(id: Long): Boolean = session {
    it.single(queryOf("select id from kafka_consumer_record where id = ?", id)) { row -> row.long("id") } != null
}

private fun Database.insertTask(name: String, instance: String, failures: Int = 1, picked: Boolean = false) = session {
    it.update(
        queryOf(
            """
                insert into scheduled_tasks
                    (task_name, task_instance, task_data, execution_time, picked, consecutive_failures, version)
                values (?, ?, ?, current_timestamp, ?, ?, 1)
            """.trimIndent(),
            name,
            instance,
            """{"id":"test"}""".toByteArray(),
            picked,
            failures,
        ),
    )
}

private fun Database.taskExists(name: String, instance: String): Boolean = session {
    it.single(
        queryOf(
            "select task_name from scheduled_tasks where task_name = ? and task_instance = ?",
            name,
            instance,
        ),
    ) { row -> row.string("task_name") } != null
}
