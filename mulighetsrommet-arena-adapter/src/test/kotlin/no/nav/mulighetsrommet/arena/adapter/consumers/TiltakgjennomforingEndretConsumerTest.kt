package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.Pending
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.Processed
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

            val event = createConsumer(listener.db, engine).processEvent(createEvent(Insert))

            event.status shouldBe Pending

            listener.assertThat("tiltaksgjennomforing").isEmpty
        }

        test("should save the event with status Pending when dependent sak is missing") {
            val saker = SakRepository(listener.db)
            saker.upsert(Sak(sakId = 13572352, lopenummer = 123, aar = 2022))

            val engine = MockEngine { respondOk() }

            val event = createConsumer(listener.db, engine).processEvent(createEvent(Insert))

            event.status shouldBe Pending

            listener.assertThat("tiltaksgjennomforing").isEmpty
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

        test("CRUD") {
            val engine = MockEngine { respondOk() }

            val consumer = createConsumer(listener.db, engine)

            val e1 = consumer.processEvent(createEvent(Insert, name = "Navn 1"))
            e1.status shouldBe Processed
            listener.assertThat("tiltaksgjennomforing")
                .row()
                .value("navn").isEqualTo("Navn 1")

            val e2 = consumer.processEvent(createEvent(Update, name = "Navn 2"))
            e2.status shouldBe Processed
            listener.assertThat("tiltaksgjennomforing")
                .row()
                .value("navn").isEqualTo("Navn 2")

            val e3 = consumer.processEvent(createEvent(Delete))
            e3.status shouldBe Processed
            listener.assertThat("tiltaksgjennomforing").isEmpty
        }

        test("should call api with mapped event payload") {
            val engine = MockEngine { respondOk() }

            createConsumer(listener.db, engine).processEvent(createEvent(Insert))

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

        test("should treat a 500 response as error") {
            val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }

            val consumer = createConsumer(listener.db, engine)

            shouldThrow<ResponseException> {
                consumer.processEvent(createEvent(Insert))
            }

            listener.assertThat("arena_events")
                .row()
                .value("consumption_status").isEqualTo("Pending")
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

private fun createEvent(operation: ArenaEventData.Operation, name: String = "Testenavn") = createArenaEvent(
    "tiltaksgjennomforing",
    "3780431",
    operation,
    """{
        "TILTAKGJENNOMFORING_ID": 3780431,
        "LOKALTNAVN": "$name",
        "TILTAKSKODE": "INDOPPFAG",
        "ARBGIV_ID_ARRANGOR": 49612,
        "SAK_ID": 13572352,
        "DATO_FRA": null,
        "DATO_TIL": null,
        "STATUS_TREVERDIKODE_INNSOKNING": "J",
        "ANTALL_DELTAKERE": 5
    }"""
)
