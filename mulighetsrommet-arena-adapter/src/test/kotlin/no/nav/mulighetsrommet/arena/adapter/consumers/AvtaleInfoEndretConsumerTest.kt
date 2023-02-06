package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema

class AvtaleInfoEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    context("when dependent events has not been processed") {
        test("should save the event with status Failed when dependent tiltakstype is missing") {
            val consumer = createConsumer(database.db)

            val event = consumer.processEvent(createEvent(ArenaEventData.Operation.Insert))

            event.status shouldBe ArenaEvent.ConsumptionStatus.Failed
            database.assertThat("avtale").isEmpty
        }
    }

    context("when dependent tiltakstype has been processed") {
        beforeEach {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(TiltakstypeFixtures.Gruppe)
        }

        test("should treat all operations as upserts") {
            val consumer = createConsumer(database.db)

            val e1 = consumer.processEvent(createEvent(ArenaEventData.Operation.Insert))
            e1.status shouldBe ArenaEvent.ConsumptionStatus.Processed
            database.assertThat("avtale").row()
                .value("status").isEqualTo("Gjennomforer")

            val e2 = consumer.processEvent(createEvent(ArenaEventData.Operation.Update, status = "PLAN"))
            e2.status shouldBe ArenaEvent.ConsumptionStatus.Processed
            database.assertThat("avtale").row()
                .value("status").isEqualTo("Planlagt")

            val e3 = consumer.processEvent(createEvent(ArenaEventData.Operation.Delete, status = "AVSLU"))
            e3.status shouldBe ArenaEvent.ConsumptionStatus.Processed
            database.assertThat("avtale").row()
                .value("status").isEqualTo("Avsluttet")
        }
    }
})

private fun createConsumer(db: Database): AvtaleInfoEndretConsumer {
    val entities = ArenaEntityService(
        events = ArenaEventRepository(db),
        mappings = ArenaEntityMappingRepository(db),
        tiltakstyper = TiltakstypeRepository(db),
        saker = SakRepository(db),
        tiltaksgjennomforinger = TiltaksgjennomforingRepository(db),
        deltakere = DeltakerRepository(db),
        avtaler = AvtaleRepository(db),
    )

    return AvtaleInfoEndretConsumer(
        ConsumerConfig("avtaleinfoendret", "avtaleinfoendret"),
        ArenaEventRepository(db),
        entities,
    )
}

private fun createEvent(
    operation: ArenaEventData.Operation,
    status: String = "GJENF"
): ArenaEvent {
    val id = 1000
    return createArenaEvent(
        ArenaTables.Sak,
        id.toString(),
        operation,
        """{
            "AVTALE_ID": $id,
            "AAR": 2022,
            "LOPENRAVTALE": 2000,
            "AVTALENAVN": "Avtale",
            "ARKIVREF": "websak",
            "ARBGIV_ID_LEVERANDOR": 1,
            "PRIS_BETBETINGELSER": "Over 9000",
            "DATO_FRA": "2022-01-04 00:00:00",
            "DATO_TIL": "2023-03-04 00:00:00",
            "TILTAKSKODE": "INDOPPFAG",
            "ORGENHET_ANSVARLIG": "2990",
            "BRUKER_ID_ANSVARLIG": "SIAMO",
            "TEKST_ANDREOPPL": null,
            "TEKST_FAGINNHOLD": "Faginnhold",
            "TEKST_MAALGRUPPE": "Alle sammen",
            "AVTALEKODE": "AVT",
            "AVTALESTATUSKODE": "$status",
            "STATUS_DATO_ENDRET": "2023-01-05 00:00:00",
            "REG_DATO": "2022-10-04 00:00:00",
            "REG_USER": "SIAMO",
            "MOD_DATO": "2022-10-05 00:00:00",
            "MOD_USER": "SIAMO",
            "PROFILELEMENT_ID_OPPL_TILTAK": 3000
        }"""
    )
}
