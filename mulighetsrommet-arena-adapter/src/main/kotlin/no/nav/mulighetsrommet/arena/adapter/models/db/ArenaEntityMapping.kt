package no.nav.mulighetsrommet.arena.adapter.models.db

import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import java.util.*

data class ArenaEntityMapping(
    val arenaTable: ArenaTable,
    val arenaId: String,
    val entityId: UUID,
    val status: Status
) {
    enum class Status {
        UPSERTED,
        IGNORED,
        UNHANDLED;
    }
}
