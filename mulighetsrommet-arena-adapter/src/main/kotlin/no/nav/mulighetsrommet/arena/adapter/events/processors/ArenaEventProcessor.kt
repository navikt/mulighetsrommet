package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ProcessingResult

interface ArenaEventProcessor {
    val arenaTable: ArenaTable

    suspend fun handleEvent(event: ArenaEvent): Either<ProcessingError, ProcessingResult>

    suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit>
}
