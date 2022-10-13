package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
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

    val listener = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        listener.db.migrate()
    }

    afterEach {
        listener.db.clean()
    }

    context("when dependent events has not been processed") {
        test("should save the event with status Pending when the dependent tiltaksgjennomføring is missing") {
            val engine = MockEngine { respondOk() }

            val event = createConsumer(listener.db, engine).processEvent(createEvent())

            event.status shouldBe ArenaEvent.ConsumptionStatus.Pending
        }
    }

    context("when dependent events has been processed") {
        beforeEach {
            val saker = SakRepository(listener.db)
            saker.upsert(Sak(sakId = 1, lopenummer = 123, aar = 2022))

            val tiltakstyper = TiltakstypeRepository(listener.db)
            tiltakstyper.upsert(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "Oppfølging",
                    innsatsgruppe = 2,
                    tiltakskode = "INDOPPFAG"
                )
            )

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(listener.db)
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

        test("should call api with mapped event payload") {
            val engine = MockEngine { respondOk() }

            createConsumer(listener.db, engine).processEvent(createEvent())

            engine.requestHistory.last().run {
                method shouldBe HttpMethod.Put

                decodeRequestBody<AdapterTiltakdeltaker>() shouldBe AdapterTiltakdeltaker(
                    tiltaksdeltakerId = 1,
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
                createConsumer(listener.db, engine).processEvent(event)

                engine.requestHistory shouldHaveSize 1
            }
        }

        context("when api returns 500") {
            test("should treat the result as error") {
                val consumer = createConsumer(
                    listener.db,
                    MockEngine { respondError(HttpStatusCode.InternalServerError) }
                )

                shouldThrow<ResponseException> {
                    consumer.processEvent(createEvent())
                }
            }
        }
    }
})

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakdeltakerEndretConsumer {
    val client = MulighetsrommetApiClient(engine, maxRetries = 0, baseUri = "api") {
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

private fun createEvent() = createArenaInsertEvent(
    "deltaker",
    "1",
    """{
        "TILTAKDELTAKER_ID": 1,
        "PERSON_ID": 2,
        "TILTAKGJENNOMFORING_ID": 3,
        "DELTAKERSTATUSKODE": "GJENN",
        "DATO_FRA": null,
        "DATO_TIL": null
    }"""
)
