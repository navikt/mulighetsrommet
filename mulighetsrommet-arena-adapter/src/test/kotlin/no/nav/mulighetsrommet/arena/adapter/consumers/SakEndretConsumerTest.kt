package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.mockk.mockk
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.SakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.domain.adapter.AdapterSak

class SakEndretConsumerTest : FunSpec({
    context("when sakskode is not TILT") {
        test("should not api with mapped event payload") {
            val engine = MockEngine { respondOk() }

            createConsumer(engine).processEvent(createEvent(sakskode = "NOT_TILT"))

            engine.requestHistory shouldHaveSize 0
        }
    }

    context("when sakskode is TILT") {
        test("should call api with mapped event payload") {
            val engine = MockEngine { respondOk() }

            createConsumer(engine).processEvent(createEvent())

            engine.requestHistory.last().run {
                method shouldBe HttpMethod.Put

                decodeRequestBody<AdapterSak>() shouldBe AdapterSak(
                    id = 1,
                    aar = 2022,
                    lopenummer = 2,
                )
            }
        }

        context("when api returns 409") {
            test("should treat the result as successful") {
                val engine = MockEngine { respondError(HttpStatusCode.Conflict) }

                createConsumer(engine).processEvent(createEvent())

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<AdapterSak>() shouldBe AdapterSak(
                        id = 1,
                        aar = 2022,
                        lopenummer = 2,
                    )
                }
            }
        }

        context("when api returns 500") {
            test("should treat the result as error") {
                val consumer = createConsumer(
                    MockEngine { respondError(HttpStatusCode.InternalServerError) }
                )

                shouldThrow<ResponseException> {
                    consumer.processEvent(createEvent())
                }
            }
        }
    }
})

private fun createConsumer(engine: HttpClientEngine): SakEndretConsumer {
    val client = MulighetsrommetApiClient(engine, maxRetries = 0, baseUri = "api") {
        "Bearer token"
    }

    val events = mockk<EventRepository>(relaxed = true)

    return SakEndretConsumer(
        ConsumerConfig("sakendret", "sakendret"),
        events,
        client
    )
}

private fun createEvent(sakskode: String = "TILT") = ArenaEvent.createInsertEvent(
    """{
        "SAK_ID": 1,
        "SAKSKODE": "$sakskode",
        "AAR": 2022,
        "LOPENRSAK": 2
    }"""
)
