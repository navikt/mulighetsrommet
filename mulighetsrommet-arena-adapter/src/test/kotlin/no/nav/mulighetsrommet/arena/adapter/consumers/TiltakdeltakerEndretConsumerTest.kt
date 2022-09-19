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
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakdeltakerEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.models.Deltakerstatus

class TiltakdeltakerEndretConsumerTest : FunSpec({

    test("should call api with mapped event payload") {
        val engine = MockEngine { respondOk() }

        createConsumer(engine).processEvent(createEvent())

        engine.requestHistory.last().run {
            method shouldBe HttpMethod.Put

            decodeRequestBody<AdapterTiltakdeltaker>() shouldBe AdapterTiltakdeltaker(
                id = 1,
                personId = 2,
                tiltaksgjennomforingId = 3,
                status = Deltakerstatus.DELTAR,
            )
        }
    }

    context("when api returns 409") {
        test("should treat the result as successful") {
            val engine = MockEngine { respondError(HttpStatusCode.Conflict) }

            val event = createEvent()
            createConsumer(engine).processEvent(event)

            engine.requestHistory shouldHaveSize 1
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

private fun createConsumer(engine: HttpClientEngine): TiltakdeltakerEndretConsumer {
    val client = MulighetsrommetApiClient(engine, maxRetries = 0, baseUri = "api") {
        "Bearer token"
    }

    val events = mockk<EventRepository>(relaxed = true)

    return TiltakdeltakerEndretConsumer(
        ConsumerConfig("deltaker", "deltaker"),
        events,
        client
    )
}

private fun createEvent() = ArenaEvent.createInsertEvent(
    """{
        "TILTAKDELTAKER_ID": 1,
        "PERSON_ID": 2,
        "TILTAKGJENNOMFORING_ID": 3,
        "DELTAKERSTATUSKODE": "GJENN",
        "DATO_FRA": null,
        "DATO_TIL": null
    }"""
)
