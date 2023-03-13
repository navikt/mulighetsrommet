package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.*
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import java.util.*

class ArenaEntityService(
    private val events: ArenaEventRepository,
    private val mappings: ArenaEntityMappingRepository,
    private val tiltakstyper: TiltakstypeRepository,
    private val saker: SakRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val deltakere: DeltakerRepository,
    private val avtaler: AvtaleRepository,
) {

    fun getEvent(arenaTable: ArenaTable, arenaId: String): Either<ProcessingError, ArenaEvent> {
        return events.get(arenaTable, arenaId)?.right() ?: ProcessingError
            .MissingDependency("ArenaEntityMapping mangler for arenaTable=$arenaTable og arenaId=$arenaId")
            .left()
    }

    fun getOrCreateMapping(event: ArenaEvent): ArenaEntityMapping {
        return mappings.get(event.arenaTable, event.arenaId)
            ?: mappings.upsert(ArenaEntityMapping(event.arenaTable, event.arenaId, UUID.randomUUID(), ArenaEntityMapping.Status.Unhandled))
    }

    fun upsertMapping(arenaEntityMapping: ArenaEntityMapping): ArenaEntityMapping {
        return mappings.upsert(arenaEntityMapping)
    }

    fun getMapping(arenaTable: ArenaTable, arenaId: String): Either<ProcessingError, ArenaEntityMapping> {
        return mappings.get(arenaTable, arenaId)?.right() ?: ProcessingError
            .MissingDependency("ArenaEntityMapping mangler for arenaTable=$arenaTable og arenaId=$arenaId")
            .left()
    }

    fun getMappingIfProcessed(arenaTable: ArenaTable, arenaId: String): ArenaEntityMapping? {
        return mappings.get(arenaTable, arenaId)
            .takeIf { events.get(arenaTable, arenaId)?.status == ArenaEvent.ProcessingStatus.Processed }
    }

    fun upsertTiltakstype(tiltakstype: Tiltakstype): Either<ProcessingError, Tiltakstype> {
        return tiltakstyper.upsert(tiltakstype)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }

    fun deleteTiltakstype(id: UUID): Either<ProcessingError, Unit> {
        return tiltakstyper.delete(id)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }

    fun getTiltakstype(id: UUID): Either<ProcessingError, Tiltakstype> {
        return tiltakstyper.get(id)?.right() ?: ProcessingError
            .MissingDependency("Tiltakstype med id=$id mangler")
            .left()
    }

    fun upsertSak(sak: Sak): Either<ProcessingError, Sak> {
        return saker.upsert(sak)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }

    fun getSak(id: Int): Either<ProcessingError.MissingDependency, Sak> {
        return saker.get(id)?.right() ?: ProcessingError.MissingDependency("Sak med id=$id mangler").left()
    }

    fun deleteSak(id: Int): Either<ProcessingError, Unit> {
        return saker.delete(id)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }

    fun upsertTiltaksgjennomforing(tiltaksgjennomforing: Tiltaksgjennomforing): Either<ProcessingError, Tiltaksgjennomforing> {
        return tiltaksgjennomforinger.upsert(tiltaksgjennomforing)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }

    fun deleteTiltaksgjennomforing(id: UUID): Either<ProcessingError, Unit> {
        return tiltaksgjennomforinger.delete(id)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }

    fun getTiltaksgjennomforingOrNull(id: UUID): Tiltaksgjennomforing? {
        return tiltaksgjennomforinger.get(id)
    }

    fun getTiltaksgjennomforing(id: UUID): Either<ProcessingError, Tiltaksgjennomforing> {
        return tiltaksgjennomforinger.get(id)?.right() ?: ProcessingError
            .MissingDependency("Tiltaksgjennomforing med id=$id mangler")
            .left()
    }

    fun isIgnored(arenaTable: ArenaTable, arenaId: String): Either<ProcessingError, Boolean> {
        // TODO: burde status Ignored settes på ArenaEntityMapping i stedet?
        //       Da har vi mulighet til å slette data fra events-tabellen, samtidig som vi har oversikt over hvilke entitier som ikke er relevante
        return getMapping(arenaTable, arenaId)
            .map { it.status == ArenaEntityMapping.Status.Ignored }
    }

    fun upsertDeltaker(deltaker: Deltaker): Either<ProcessingError, Deltaker> {
        return deltakere.upsert(deltaker)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }

    fun deleteDeltaker(id: UUID): Either<ProcessingError, Unit> {
        return deltakere.delete(id)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }

    fun upsertAvtale(avtale: Avtale): Either<ProcessingError, Avtale> {
        return avtaler.upsert(avtale)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }

    fun deleteAvtale(id: UUID): Either<ProcessingError, Unit> {
        return avtaler.delete(id)
            .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
    }
}
