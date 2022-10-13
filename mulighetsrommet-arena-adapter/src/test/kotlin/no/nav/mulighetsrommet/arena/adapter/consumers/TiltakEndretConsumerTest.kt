package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.mockk.mockk
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak

class TiltakEndretConsumerTest : FunSpec({
    test("should call api with mapped event payload") {
        val engine = MockEngine { respondOk() }

        createConsumer(engine).processEvent(createEvent())

        engine.requestHistory.last().run {
            method shouldBe HttpMethod.Put

            decodeRequestBody<AdapterTiltak>() shouldBe AdapterTiltak(
                navn = "Oppfølging",
                innsatsgruppe = 2,
                tiltakskode = "INDOPPFAG",
            )
        }
    }

    context("when api returns 409") {
        test("should treat the result as error") {
            val consumer = createConsumer(
                MockEngine { respondError(HttpStatusCode.Conflict) }
            )

            shouldThrow<ResponseException> {
                consumer.processEvent(createEvent())
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
})

private fun createConsumer(engine: HttpClientEngine): TiltakEndretConsumer {
    val client = MulighetsrommetApiClient(engine, maxRetries = 0, baseUri = "api") {
        "Bearer token"
    }

    val events = mockk<EventRepository>(relaxed = true)

    return TiltakEndretConsumer(
        ConsumerConfig("tiltakendret", "tiltakendret"),
        events,
        client
    )
}

private fun createEvent() = ArenaEvent.createInsertEvent(
    """{
        "TILTAKSNAVN": "Oppfølging",
        "TILTAKSKODE": "INDOPPFAG",
        "DATO_FRA": null,
        "DATO_TIL": null
    }"""
)
