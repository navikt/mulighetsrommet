package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing

class TiltakgjennomforingEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        listener.db.migrate()
    }

    afterEach {
        listener.db.clean()
    }

    context("when dependent events has been processed") {

        test("should call api with mapped event payload") {
            val engine = MockEngine { respondOk() }

            createConsumer(listener.db, engine).processEvent(createEvent())

            engine.requestHistory.last().run {
                method shouldBe HttpMethod.Put

                decodeRequestBody<AdapterTiltaksgjennomforing>() shouldBe AdapterTiltaksgjennomforing(
                    tiltaksgjennomforingId = 3780431,
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
        }

        context("when api returns 500") {
            test("should treat the result as error") {
                val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }

                val consumer = createConsumer(listener.db, engine)

                shouldThrow<ResponseException> {
                    consumer.processEvent(createEvent())
                }
            }
        }
    }
})

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakgjennomforingEndretConsumer {
    val client = MulighetsrommetApiClient(engine, maxRetries = 0, baseUri = "api") {
        "Bearer token"
    }

    return TiltakgjennomforingEndretConsumer(
        ConsumerConfig("tiltakgjennomforing", "tiltakgjennomforing"),
        ArenaEventRepository(db),
        client
    )
}

private fun createEvent() = createArenaInsertEvent(
    "tiltaksgjennomforing",
    "3780431",
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
