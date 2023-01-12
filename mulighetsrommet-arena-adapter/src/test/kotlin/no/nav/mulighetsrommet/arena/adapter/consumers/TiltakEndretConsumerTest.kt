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
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.Failed
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.Processed
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import java.time.LocalDate

class TiltakEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    test("should treat all operations as upserts") {
        val consumer = createConsumer(database.db, MockEngine { respondOk() })

        val e1 = consumer.processEvent(createEvent(Insert, name = "Oppfølging 1"))
        e1.status shouldBe Processed
        database.assertThat("tiltakstype")
            .row().value("navn").isEqualTo("Oppfølging 1")

        val e2 = consumer.processEvent(createEvent(Update, name = "Oppfølging 2"))
        e2.status shouldBe Processed
        database.assertThat("tiltakstype")
            .row().value("navn").isEqualTo("Oppfølging 2")

        val e3 = consumer.processEvent(createEvent(Delete, name = "Oppfølging 1"))
        e3.status shouldBe Processed
        database.assertThat("tiltakstype")
            .row().value("navn").isEqualTo("Oppfølging 1")

        database.assertThat("tiltakstype")
            .row().value("rett_paa_tiltakspenger").isEqualTo(true)

        database.assertThat("tiltakstype")
            .row().value("fra_dato").isEqualTo(LocalDate.of(2022, 1, 11))

        database.assertThat("tiltakstype")
            .row().value("til_dato").isEqualTo(LocalDate.of(2022, 1, 15))
    }

    context("api responses") {
        test("should call api with mapped event payload") {
            val engine = MockEngine { respondOk() }
            val consumer = createConsumer(database.db, engine)

            consumer.processEvent(createEvent(Insert))

            val generatedId = engine.requestHistory.last().run {
                method shouldBe HttpMethod.Put

                val tiltakstype = decodeRequestBody<TiltakstypeDbo>().apply {
                    navn shouldBe "Oppfølging"
                    rettPaaTiltakspenger shouldBe true
                }

                tiltakstype.id
            }

            consumer.processEvent(createEvent(Delete))

            engine.requestHistory.last().run {
                method shouldBe HttpMethod.Delete

                decodeRequestBody<TiltakstypeDbo>().apply {
                    id shouldBe generatedId
                }
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
})

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakEndretConsumer {
    val client = MulighetsrommetApiClient(engine, baseUri = "api") {
        "Bearer token"
    }

    val entities = ArenaEntityService(
        events = ArenaEventRepository(db),
        mappings = ArenaEntityMappingRepository(db),
        tiltakstyper = TiltakstypeRepository(db),
        saker = SakRepository(db),
        tiltaksgjennomforinger = TiltaksgjennomforingRepository(db),
        deltakere = DeltakerRepository(db)
    )

    return TiltakEndretConsumer(
        ConsumerConfig("tiltakendret", "tiltakendret"),
        ArenaEventRepository(db),
        entities,
        client
    )
}

private fun createEvent(operation: ArenaEventData.Operation = Insert, name: String = "Oppfølging") = createArenaEvent(
    ArenaTables.Tiltakstype,
    "INDOPPFAG",
    operation,
    """{
        "TILTAKSNAVN": "$name",
        "TILTAKSKODE": "INDOPPFAG",
        "DATO_FRA": "2022-01-11 00:00:00",
        "DATO_TIL": "2022-01-15 00:00:00",
        "STATUS_BASISYTELSE": "J"
    }"""
)
