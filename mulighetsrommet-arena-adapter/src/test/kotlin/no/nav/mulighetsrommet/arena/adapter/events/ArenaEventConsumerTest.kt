package no.nav.mulighetsrommet.arena.adapter.events

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.arena.adapter.fixtures.*
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.*

class ArenaEventConsumerTest : FunSpec({
    test("decode ArenaEvent") {
        forAll(
            row(
                createArenaTiltakEvent(Delete) { it.copy(TILTAKSKODE = "TILTAK") },
                Delete,
                Tiltakstype,
                "TILTAK",
            ),
            row(
                createArenaSakEvent(Insert) { it.copy(SAK_ID = 1) },
                Insert,
                Sak,
                "1",
            ),
            row(
                createArenaAvtaleInfoEvent(Update) { it.copy(AVTALE_ID = 2) },
                Update,
                AvtaleInfo,
                "2",
            ),
            row(
                createArenaTiltakgjennomforingEvent(Insert) { it.copy(TILTAKGJENNOMFORING_ID = 3) },
                Insert,
                Tiltaksgjennomforing,
                "3",
            ),
            row(
                createArenaTiltakdeltakerEvent(Insert) { it.copy(TILTAKDELTAKER_ID = 4) },
                Insert,
                Deltaker,
                "4",
            ),
        ) { event, operation, table, id ->
            val decoded = decodeArenaEvent(event.payload)

            decoded.operation shouldBe operation
            decoded.arenaTable shouldBe table
            decoded.arenaId shouldBe id
        }
    }
})
