package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent

interface ArenaEventProcessor {
    val arenaTable: ArenaTable

    suspend fun handleEvent(event: ArenaEvent): Either<ProcessingError, ArenaEvent.ProcessingStatus>

    suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit>
}
