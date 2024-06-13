package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent

interface ArenaEventProcessor {
    suspend fun shouldHandleEvent(event: ArenaEvent): Boolean

    suspend fun handleEvent(event: ArenaEvent): Either<ProcessingError, ProcessingResult>

    suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit>

    fun getDependentEntities(event: ArenaEvent): List<ArenaEntityMapping> = emptyList()
}
