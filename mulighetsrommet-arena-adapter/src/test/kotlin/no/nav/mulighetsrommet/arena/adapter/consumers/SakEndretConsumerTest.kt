package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.Processed
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.SakRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema

class SakEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        listener.db.migrate()
    }

    afterEach {
        listener.db.clean()
    }

    context("when sakskode is not TILT") {
        test("should ignore events") {
            val event = createConsumer(listener.db).processEvent(
                createEvent(
                    sakskode = "NOT_TILT",
                    operation = Insert
                )
            )

            event.status shouldBe Ignored
        }
    }

    context("when sakskode is TILT") {
        test("should treat all operations as upserts") {
            val consumer = createConsumer(listener.db)

            val e1 = consumer.processEvent(createEvent(Insert, lopenummer = 1))
            e1.status shouldBe Processed
            listener.assertThat("sak")
                .row().value("lopenummer").isEqualTo(1)

            val e2 = consumer.processEvent(createEvent(Update, lopenummer = 2))
            e2.status shouldBe Processed
            listener.assertThat("sak")
                .row().value("lopenummer").isEqualTo(2)

            val e3 = consumer.processEvent(createEvent(Delete, lopenummer = 1))
            e3.status shouldBe Processed
            listener.assertThat("sak")
                .row().value("lopenummer").isEqualTo(1)
        }
    }
})

private fun createConsumer(db: Database): SakEndretConsumer {
    return SakEndretConsumer(
        ConsumerConfig("sakendret", "sakendret"),
        ArenaEventRepository(db),
        SakRepository(db),
    )
}

private fun createEvent(
    operation: ArenaEventData.Operation,
    sakskode: String = "TILT",
    lopenummer: Int = 1
) = createArenaEvent(
    ArenaTables.Sak,
    "1",
    operation,
    """{
        "SAK_ID": 1,
        "SAKSKODE": "$sakskode",
        "AAR": 2022,
        "LOPENRSAK": $lopenummer
    }"""
)
