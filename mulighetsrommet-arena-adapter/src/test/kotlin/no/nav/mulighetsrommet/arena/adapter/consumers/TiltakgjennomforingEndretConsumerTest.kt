package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.*
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import java.util.*

class TiltakgjennomforingEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    context("when dependent events has not been processed") {
        test("should save the event with status Failed when dependent tiltakstype is missing") {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "Oppfølging",
                    innsatsgruppe = 2,
                    tiltakskode = "INDOPPFAG"
                )
            )

            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createEvent(Insert))

            event.status shouldBe Failed
            database.assertThat("tiltaksgjennomforing").isEmpty
        }

        test("should save the event with status Failed when dependent sak is missing") {
            val saker = SakRepository(database.db)
            saker.upsert(Sak(sakId = 13572352, lopenummer = 123, aar = 2022))

            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createEvent(Insert))

            event.status shouldBe Failed
            database.assertThat("tiltaksgjennomforing").isEmpty
        }
    }

    context("when dependent events has been processed") {
        beforeEach {
            val saker = SakRepository(database.db)
            saker.upsert(Sak(sakId = 13572352, lopenummer = 123, aar = 2022))

            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "Oppfølging",
                    innsatsgruppe = 2,
                    tiltakskode = "INDOPPFAG"
                )
            )
        }

        test("should treat all operations as upserts") {
            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val e1 = consumer.processEvent(createEvent(Insert, name = "Navn 1"))
            e1.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing")
                .row().value("navn").isEqualTo("Navn 1")

            val e2 = consumer.processEvent(createEvent(Update, name = "Navn 2"))
            e2.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing")
                .row().value("navn").isEqualTo("Navn 2")

            val e3 = consumer.processEvent(createEvent(Delete, name = "Navn 1"))
            e3.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing")
                .row().value("navn").isEqualTo("Navn 1")
        }

        test("should ignore tiltaksgjennomføringer older than Aktivitetsplanen") {
            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createEvent(Insert, regDato = "2017-12-03 23:59:59"))

            event.status shouldBe Ignored
        }

        context("api responses") {
            test("should call api with mapped event payload") {
                val engine = MockEngine { respondOk() }
                val consumer = createConsumer(database.db, engine)

                consumer.processEvent(createEvent(Insert))

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put
                    // TODO: assert payload?
                }

                consumer.processEvent(createEvent(Delete))

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Delete
                    // TODO: assert payload?
                }
            }

            test("should treat a 500 response as error") {
                val consumer = createConsumer(
                    database.db,
                    MockEngine { respondError(HttpStatusCode.InternalServerError) }
                )

                val event = consumer.processEvent(createEvent(Insert))

                event.status shouldBe Failed
                database.assertThat("arena_events")
                    .row()
                    .value("consumption_status").isEqualTo("Failed")
            }
        }
    }
})

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakgjennomforingEndretConsumer {
    val client = MulighetsrommetApiClient(engine, baseUri = "api") {
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

private fun createEvent(
    operation: ArenaEventData.Operation,
    name: String = "Testenavn",
    regDato: String = "2022-10-10 00:00:00"
) = createArenaEvent(
    ArenaTables.Tiltaksgjennomforing,
    "3780431",
    operation,
    """{
        "TILTAKGJENNOMFORING_ID": 3780431,
        "LOKALTNAVN": "$name",
        "TILTAKSKODE": "INDOPPFAG",
        "ARBGIV_ID_ARRANGOR": 49612,
        "SAK_ID": 13572352,
        "REG_DATO": "$regDato",
        "DATO_FRA": null,
        "DATO_TIL": null,
        "STATUS_TREVERDIKODE_INNSOKNING": "J",
        "ANTALL_DELTAKERE": 5
    }"""
)
