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
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.*
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.models.Deltakerstatus
import java.util.*

class TiltakdeltakerEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    context("when dependent events has not been processed") {
        test("should save the event with status Pending when the dependent tiltaksgjennomføring is missing") {
            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createEvent(Insert))

            event.status shouldBe Pending
            database.assertThat("deltaker").isEmpty
        }
    }

    context("when dependent events has been processed") {
        beforeEach {
            val saker = SakRepository(database.db)
            saker.upsert(Sak(sakId = 1, lopenummer = 123, aar = 2022))

            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "Oppfølging",
                    innsatsgruppe = 2,
                    tiltakskode = "INDOPPFAG"
                )
            )

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(
                Tiltaksgjennomforing(
                    id = UUID.randomUUID(),
                    tiltaksgjennomforingId = 3,
                    sakId = 1,
                    tiltakskode = "INDOPPFAG",
                    arrangorId = null,
                    navn = null,
                )
            )
        }

        test("should treat all operations as upserts") {
            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val e1 = consumer.processEvent(createEvent(Insert, status = "GJENN"))
            e1.status shouldBe Processed
            database.assertThat("deltaker")
                .row().value("status").isEqualTo("DELTAR")

            val e2 = consumer.processEvent(createEvent(Update, status = "FULLF"))
            e2.status shouldBe Processed
            database.assertThat("deltaker")
                .row().value("status").isEqualTo("AVSLUTTET")

            val e3 = consumer.processEvent(createEvent(Delete, status = "FULLF"))
            e3.status shouldBe Processed
            database.assertThat("deltaker")
                .row().value("status").isEqualTo("AVSLUTTET")
        }

        context("api responses") {
            test("should call api with mapped event payload") {
                val engine = MockEngine { respondOk() }
                val consumer = createConsumer(database.db, engine)

                val deltaker = AdapterTiltakdeltaker(
                    tiltaksdeltakerId = 1,
                    personId = 2,
                    tiltaksgjennomforingId = 3,
                    status = Deltakerstatus.DELTAR,
                )

                consumer.processEvent(createEvent(Insert))

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put
                    decodeRequestBody<AdapterTiltakdeltaker>() shouldBe deltaker
                }

                consumer.processEvent(createEvent(Delete))

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Delete
                    decodeRequestBody<AdapterTiltakdeltaker>() shouldBe deltaker
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

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakdeltakerEndretConsumer {
    val client = MulighetsrommetApiClient(engine, baseUri = "api") {
        "Bearer token"
    }

    return TiltakdeltakerEndretConsumer(
        ConsumerConfig("deltaker", "deltaker"),
        ArenaEventRepository(db),
        DeltakerRepository(db),
        ArenaEntityMappingRepository(db),
        client
    )
}

private fun createEvent(operation: ArenaEventData.Operation, status: String = "GJENN") = createArenaEvent(
    "deltaker",
    "1",
    operation,
    """{
        "TILTAKDELTAKER_ID": 1,
        "PERSON_ID": 2,
        "TILTAKGJENNOMFORING_ID": 3,
        "DELTAKERSTATUSKODE": "$status",
        "DATO_FRA": null,
        "DATO_TIL": null
    }"""
)
