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
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakgjennomforingEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing

class TiltakgjennomforingEndretConsumerTest : FunSpec({

    test("should call api with mapped event payload") {
        val engine = MockEngine { respondOk() }

        val event = createtiltaksgjennomforingEndretEvent(antallDeltakere = 5)
        createTiltaksgjennomforingEndretConsumer(engine).processEvent(event)

        val request = engine.requestHistory.last()
        request.method shouldBe HttpMethod.Put
        decodeRequestBody<AdapterTiltaksgjennomforing>(request) shouldBe AdapterTiltaksgjennomforing(
            id = 3780431,
            navn = "Testenavn",
            tiltakskode = "INDOPPFAG",
            arrangorId = 49612,
            sakId = 13572352,
            fraDato = null,
            tilDato = null,
            apentForInnsok = true,
            antallPlasser = 5,
        )
    }

    test("should decode ANTALL_DELTAKERE as nearest integer") {
        val engine = MockEngine { respondOk() }

        val event = createtiltaksgjennomforingEndretEvent(antallDeltakere = 5.5)
        createTiltaksgjennomforingEndretConsumer(engine).processEvent(event)

        decodeRequestBody<AdapterTiltaksgjennomforing>(engine.requestHistory.last()).antallPlasser shouldBe 6
    }

    context("when api returns 500") {
        test("should treat the result as error") {
            val consumer = createTiltaksgjennomforingEndretConsumer(
                MockEngine { respondError(HttpStatusCode.InternalServerError) }
            )

            shouldThrow<ResponseException> {
                consumer.processEvent(createtiltaksgjennomforingEndretEvent())
            }
        }
    }
})

fun createTiltaksgjennomforingEndretConsumer(engine: HttpClientEngine): TiltakgjennomforingEndretConsumer {
    val client = MulighetsrommetApiClient(engine, maxRetries = 1, baseUri = "api") {
        "Bearer token"
    }

    val events = mockk<EventRepository>(relaxed = true)

    return TiltakgjennomforingEndretConsumer(
        ConsumerConfig("tiltakgjennomforing", "tiltakgjennomforing"),
        events,
        client
    )
}

private fun createtiltaksgjennomforingEndretEvent(antallDeltakere: Number = 5) = ArenaEvent.createInsertEvent(
    """{
        "TILTAKGJENNOMFORING_ID": 3780431,
        "LOKALTNAVN": "Testenavn",
        "TILTAKSKODE": "INDOPPFAG",
        "ARBGIV_ID_ARRANGOR": 49612,
        "SAK_ID": 13572352,
        "DATO_FRA": null,
        "DATO_TIL": null,
        "STATUS_TREVERDIKODE_INNSOKNING": "J",
        "ANTALL_DELTAKERE": $antallDeltakere
    }"""
)
