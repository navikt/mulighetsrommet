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
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import java.util.*

class TiltakgjennomforingEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        listener.db.migrate()
    }

    afterEach {
        listener.db.clean()
    }

    context("when dependent events has not been processed") {
        test("should save the event with status Pending when dependent tiltakstype is missing") {
            val tiltakstyper = TiltakstypeRepository(listener.db)
            tiltakstyper.upsert(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "Oppfølging",
                    innsatsgruppe = 2,
                    tiltakskode = "INDOPPFAG"
                )
            )

            val engine = MockEngine { respondOk() }

            val event = createConsumer(listener.db, engine).processEvent(createEvent())

            event.status shouldBe ArenaEvent.ConsumptionStatus.Pending
        }

        test("should save the event with status Pending when dependent sak is missing") {
            val saker = SakRepository(listener.db)
            saker.upsert(Sak(sakId = 13572352, lopenummer = 123, aar = 2022))

            val engine = MockEngine { respondOk() }

            val event = createConsumer(listener.db, engine).processEvent(createEvent())

            event.status shouldBe ArenaEvent.ConsumptionStatus.Pending
        }
    }

    context("when dependent events has been processed") {
        beforeEach {
            val saker = SakRepository(listener.db)
            saker.upsert(Sak(sakId = 13572352, lopenummer = 123, aar = 2022))

            val tiltakstyper = TiltakstypeRepository(listener.db)
            tiltakstyper.upsert(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "Oppfølging",
                    innsatsgruppe = 2,
                    tiltakskode = "INDOPPFAG"
                )
            )
        }

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
        TiltaksgjennomforingRepository(db),
        ArenaEntityMappingRepository(db),
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
