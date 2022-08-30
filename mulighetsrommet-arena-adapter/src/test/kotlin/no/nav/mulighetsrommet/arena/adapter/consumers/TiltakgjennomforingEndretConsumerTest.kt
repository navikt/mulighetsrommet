package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.mockk.mockk
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakgjennomforingEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing

class TiltakgjennomforingEndretConsumerTest : FunSpec({

    val engine = MockEngine {
        respond(
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
            status = HttpStatusCode.OK,
            content = ByteReadChannel("{}"),
        )
    }
    val client = MulighetsrommetApiClient(engine, baseUri = "api") {
        "Bearer token"
    }

    val events = mockk<EventRepository>(relaxed = true)

    val consumer = TiltakgjennomforingEndretConsumer(
        ConsumerConfig("tiltakgjennomforing", "tiltakgjennomforing"),
        events,
        client
    )

    test("should call api with mapped event payload") {
        val event = createInsertEvent(
            """{
                "TILTAKGJENNOMFORING_ID": 3780431,
                "LOKALTNAVN": "Testenavn",
                "TILTAKSKODE": "INDOPPFAG",
                "ARBGIV_ID_ARRANGOR": 49612,
                "SAK_ID": 13572352,
                "DATO_FRA": null,
                "DATO_TIL": null,
                "STATUS_TREVERDIKODE_INNSOKNING": "J",
                "ANTALL_DELTAKERE": 5
            }"""
        )

        consumer.processEvent(event)

        val request = engine.requestHistory.last()

        request.method shouldBe HttpMethod.Put
        decodeRequestBody(request, AdapterTiltaksgjennomforing.serializer()) shouldBe AdapterTiltaksgjennomforing(
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
        val event = createInsertEvent(
            """{
                "TILTAKGJENNOMFORING_ID": 3780431,
                "LOKALTNAVN": "Testenavn",
                "TILTAKSKODE": "INDOPPFAG",
                "ARBGIV_ID_ARRANGOR": 49612,
                "SAK_ID": 13572352,
                "DATO_FRA": null,
                "DATO_TIL": null,
                "STATUS_TREVERDIKODE_INNSOKNING": "J",
                "ANTALL_DELTAKERE": 5.5
            }"""
        )

        consumer.processEvent(event)

        decodeRequestBody(engine.requestHistory.last(), AdapterTiltaksgjennomforing.serializer()).antallPlasser shouldBe 6
    }
})

private fun <T> decodeRequestBody(request: HttpRequestData, kSerializer: KSerializer<T>): T {
    return Json.decodeFromString(kSerializer, (request.body as TextContent).text)
}

private fun createInsertEvent(data: String) = Json.parseToJsonElement(
    """{
    "op_type": "I",
    "after": $data
}
"""
)
