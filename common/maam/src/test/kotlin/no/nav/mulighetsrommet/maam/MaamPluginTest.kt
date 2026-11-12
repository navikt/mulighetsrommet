package no.nav.mulighetsrommet.maam

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import no.nav.mulighetsrommet.kafka.TopicType
import no.nav.mulighetsrommet.maam.dto.ExecutedTaskResponse
import no.nav.mulighetsrommet.maam.dto.KafkaTopicConsumerDto
import no.nav.mulighetsrommet.maam.dto.ScheduleTaskResponse
import java.util.UUID
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class MaamPluginTest : FunSpec({
    test("eksponerer bare konfigurerte funksjoner under valgt prefiks") {
        val orchestrator = mockk<KafkaConsumerOrchestrator>()
        val topics = listOf(
            Topic("consumer-1", "input-topic", TopicType.CONSUMER, running = false),
            Topic("consumer-2", "input-topic", TopicType.CONSUMER, running = true),
        )
        val consumerDtos = listOf(
            KafkaTopicConsumerDto("consumer-1", "input-topic", running = false),
            KafkaTopicConsumerDto("consumer-2", "input-topic", running = true),
        )
        every { orchestrator.getTopics() } returns topics
        every { orchestrator.updateRunningTopics(topics) } returns topics

        withMaamPlugin({
            kafka(orchestrator = { orchestrator }) {
                consumers()
            }
        }) {
            client.get("/maam/kafka/consumers").body<List<KafkaTopicConsumerDto>>() shouldBe consumerDtos
            client.put("/maam/kafka/consumers") {
                contentType(ContentType.Application.Json)
                setBody(consumerDtos)
            }.status shouldBe HttpStatusCode.OK
            client.get("/kafka/consumers").status shouldBe HttpStatusCode.NotFound
            client.get("/maam/kafka/failed-records").status shouldBe HttpStatusCode.NotFound
            client.get("/maam/tasks/scheduled/failed").status shouldBe HttpStatusCode.NotFound
        }

        verify(exactly = 1) { orchestrator.updateRunningTopics(topics) }
    }

    test("oppslag av avhengigheter og kjøring av tasks skjer ikke ved installasjon") {
        withMaamPlugin({
            database = { error("Database must not be resolved during installation") }
            kafka(orchestrator = { error("Kafka must not be resolved during installation") }) {
                consumers()
                failedRecords()
            }
            tasks {
                scheduled()
                task("lazy") {
                    error("Task must not run during installation")
                }
            }
        }) {
            startApplication()
        }
    }

    test("feilede Kafka-meldinger krever database ved installasjon") {
        shouldThrow<IllegalArgumentException> {
            withMaamPlugin({
                kafka(orchestrator = { mockk() }) {
                    failedRecords()
                }
            }) {
                startApplication()
            }
        }.message shouldBe "Maam requires a database provider for failed Kafka records"
    }

    test("administrasjon av planlagte tasks krever database ved installasjon") {
        shouldThrow<IllegalArgumentException> {
            withMaamPlugin({
                tasks {
                    scheduled()
                }
            }) {
                startApplication()
            }
        }.message shouldBe "Maam requires a database provider for scheduled tasks"
    }

    test("task uten input returnerer 202 og id for planlagt kjøring") {
        val taskId = UUID.fromString("00000000-0000-0000-0000-000000000123")

        withMaamPlugin({
            tasks {
                task("schedule") {
                    TaskResult.Scheduled(taskId)
                }
            }
        }) {
            val response = client.post("/maam/tasks/schedule")
            response.status shouldBe HttpStatusCode.Accepted
            response.body<ScheduleTaskResponse>().id shouldBe taskId
            client.post("/tasks/schedule").status shouldBe HttpStatusCode.NotFound
        }
    }

    test("task med typet input returnerer 200 og melding etter fullført kjøring") {
        withMaamPlugin({
            tasks {
                task<TestTaskRequest>("execute") { request ->
                    TaskResult.Completed("Utført ${request.name}")
                }
            }
        }) {
            val response = client.post("/maam/tasks/execute") {
                contentType(ContentType.Application.Json)
                setBody(TestTaskRequest("test"))
            }
            response.status shouldBe HttpStatusCode.OK
            response.body<ExecutedTaskResponse>().message shouldBe "Utført test"
        }
    }

    test("avvist task returnerer 400 og forklaring") {
        withMaamPlugin({
            tasks {
                task<TestTaskRequest>("reject") { request ->
                    TaskResult.Rejected("Ugyldig input: ${request.name}")
                }
            }
        }) {
            val response = client.post("/maam/tasks/reject") {
                contentType(ContentType.Application.Json)
                setBody(TestTaskRequest("test"))
            }
            response.status shouldBe HttpStatusCode.BadRequest
            response.body<ExecutedTaskResponse>().message shouldBe "Ugyldig input: test"
        }
    }

    test("avviser tasks med samme navn") {
        shouldThrow<IllegalArgumentException> {
            MaamTasksConfig().apply {
                task("duplicate") { TaskResult.Completed("first") }
                task("duplicate") { TaskResult.Completed("second") }
            }
        }.message shouldBe "Maam task 'duplicate' is already registered"
    }
})

@Serializable
private data class TestTaskRequest(val name: String)

private fun withMaamPlugin(
    configure: MaamConfig.() -> Unit,
    test: suspend ApplicationTestBuilder.() -> Unit,
) = testApplication {
    application {
        install(ContentNegotiation) { json() }
        routing {
            route("/maam") {
                install(Maam, configure)
            }
        }
    }
    client = createClient {
        install(ClientContentNegotiation) { json() }
    }
    test()
}
