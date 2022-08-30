package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.mockk.mockk
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.SakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.domain.adapter.AdapterSak

class SakEndretConsumerTest : FunSpec({
    context("when sakskode is not TILT") {
        test("should not call api with mapped event payload") {
            val engine = MockEngine {
                respond(
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    status = HttpStatusCode.OK,
                    content = ByteReadChannel("{}"),
                )
            }

            val event = createInsertEvent(
                """{
                    "SAK_ID": 1,
                    "SAKSKODE": "NOT_TILT",
                    "AAR": 2022,
                    "LOPENRSAK": 2
                }"""
            )

            createSakEndretConsumer(engine).processEvent(event)

            engine.requestHistory shouldHaveSize 0
        }
    }

    context("when sakskode is TILT") {
        test("should call api with mapped event payload") {
            val engine = MockEngine {
                respond(
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    status = HttpStatusCode.OK,
                    content = ByteReadChannel("{}"),
                )
            }

            val event = createInsertEvent(
                """{
                    "SAK_ID": 1,
                    "SAKSKODE": "TILT",
                    "AAR": 2022,
                    "LOPENRSAK": 2
                }"""
            )

            createSakEndretConsumer(engine).processEvent(event)

            val request = engine.requestHistory.last()

            request.method shouldBe HttpMethod.Put
            decodeRequestBody<AdapterSak>(request) shouldBe AdapterSak(
                id = 1,
                aar = 2022,
                lopenummer = 2,
            )
        }

        context("when api returns 409") {
            test("should treat the result as successful") {
                val engine = MockEngine {
                    respond(
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        status = HttpStatusCode.Conflict,
                        content = ByteReadChannel("{}"),
                    )
                }

                val consumer = createSakEndretConsumer(engine)

                val event = createInsertEvent(
                    """{
                        "SAK_ID": 1,
                        "SAKSKODE": "TILT",
                        "AAR": 2022,
                        "LOPENRSAK": 2
                    }"""
                )

                consumer.processEvent(event)

                val request = engine.requestHistory.last()

                request.method shouldBe HttpMethod.Put
                decodeRequestBody<AdapterSak>(request) shouldBe AdapterSak(
                    id = 1,
                    aar = 2022,
                    lopenummer = 2,
                )
            }
        }

        context("when api returns 500") {
            test("should treat the result as error") {
                val engine = MockEngine {
                    respond(
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        status = HttpStatusCode.InternalServerError,
                        content = ByteReadChannel("{}"),
                    )
                }

                val consumer = createSakEndretConsumer(engine)

                val event = createInsertEvent(
                    """{
                        "SAK_ID": 1,
                        "SAKSKODE": "TILT",
                        "AAR": 2022,
                        "LOPENRSAK": 2
                    }"""
                )

                shouldThrow<ResponseException> {
                    consumer.processEvent(event)
                }
            }
        }
    }
})

private fun createSakEndretConsumer(engine: HttpClientEngine): SakEndretConsumer {
    val client = MulighetsrommetApiClient(engine, maxRetries = 1, baseUri = "api") {
        "Bearer token"
    }

    val events = mockk<EventRepository>(relaxed = true)

    return SakEndretConsumer(
        ConsumerConfig("sakendret", "sakendret"),
        events,
        client
    )
}

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> decodeRequestBody(request: HttpRequestData): T {
    return Json.decodeFromString(T::class.serializer(), (request.body as TextContent).text)
}

private fun createInsertEvent(data: String) = Json.parseToJsonElement(
    """{
        "op_type": "I",
        "after": $data
    }
    """
)
