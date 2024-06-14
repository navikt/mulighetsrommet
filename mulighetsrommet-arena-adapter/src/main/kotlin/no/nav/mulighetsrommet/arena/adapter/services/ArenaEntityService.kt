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
    private val mappings: ArenaEntityMappingRepository,
    private val tiltakstyper: TiltakstypeRepository,
    private val saker: SakRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val deltakere: DeltakerRepository,
    private val avtaler: AvtaleRepository,
) {
    fun getOrCreateMapping(event: ArenaEvent): ArenaEntityMapping = mappings.get(event.arenaTable, event.arenaId)
        ?: mappings.upsert(
            ArenaEntityMapping(
                event.arenaTable,
                event.arenaId,
                event.getEksternID() ?: UUID.randomUUID(),
                when (event.status) {
                    ArenaEvent.ProcessingStatus.Processed -> ArenaEntityMapping.Status.Handled
                    else -> ArenaEntityMapping.Status.Unhandled
                },
            ),
        )

    fun upsertMapping(arenaEntityMapping: ArenaEntityMapping): ArenaEntityMapping = mappings.upsert(arenaEntityMapping)

    fun getMapping(arenaTable: ArenaTable, arenaId: String): Either<ProcessingError, ArenaEntityMapping> = mappings.get(arenaTable, arenaId)?.right() ?: ProcessingError
        .ForeignKeyViolation("ArenaEntityMapping mangler for arenaTable=$arenaTable og arenaId=$arenaId")
        .left()

    fun getMappingIfHandled(arenaTable: ArenaTable, arenaId: String): ArenaEntityMapping? = mappings.get(arenaTable, arenaId)
        .takeIf { it?.status == ArenaEntityMapping.Status.Handled }

    fun upsertTiltakstype(tiltakstype: Tiltakstype): Either<ProcessingError, Tiltakstype> = tiltakstyper.upsert(tiltakstype)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }

    fun deleteTiltakstype(id: UUID): Either<ProcessingError, Unit> = tiltakstyper.delete(id)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }

    fun getTiltakstype(id: UUID): Either<ProcessingError, Tiltakstype> = tiltakstyper.get(id)?.right() ?: ProcessingError
        .ForeignKeyViolation("Tiltakstype med id=$id mangler")
        .left()

    fun upsertSak(sak: Sak): Either<ProcessingError, Sak> = saker.upsert(sak)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }

    fun getSak(id: Int): Either<ProcessingError.ForeignKeyViolation, Sak> = saker.get(id)?.right() ?: ProcessingError.ForeignKeyViolation("Sak med id=$id mangler").left()

    fun deleteSak(id: Int): Either<ProcessingError, Unit> = saker.delete(id)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }

    fun upsertTiltaksgjennomforing(tiltaksgjennomforing: Tiltaksgjennomforing): Either<ProcessingError, Tiltaksgjennomforing> = tiltaksgjennomforinger.upsert(tiltaksgjennomforing)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }

    fun deleteTiltaksgjennomforing(id: UUID): Either<ProcessingError, Unit> = tiltaksgjennomforinger.delete(id)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }

    fun getTiltaksgjennomforingOrNull(id: UUID): Tiltaksgjennomforing? = tiltaksgjennomforinger.get(id)

    fun getTiltaksgjennomforing(id: UUID): Either<ProcessingError, Tiltaksgjennomforing> = tiltaksgjennomforinger.get(id)?.right() ?: ProcessingError
        .ForeignKeyViolation("Tiltaksgjennomforing med id=$id mangler")
        .left()

    fun isIgnored(arenaTable: ArenaTable, arenaId: String): Either<ProcessingError, Boolean> = getMapping(arenaTable, arenaId)
        .map { it.status == ArenaEntityMapping.Status.Ignored }

    fun upsertDeltaker(deltaker: Deltaker): Either<ProcessingError, Deltaker> = deltakere.upsert(deltaker)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }

    fun getDeltakereByTiltaksgjennomforingId(id: Int): List<Deltaker> = deltakere.getByTiltaksgjennomforingId(id)

    fun deleteDeltaker(id: UUID): Either<ProcessingError, Unit> = deltakere.delete(id)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }

    fun upsertAvtale(avtale: Avtale): Either<ProcessingError, Avtale> = avtaler.upsert(avtale)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }

    fun deleteAvtale(id: UUID): Either<ProcessingError, Unit> = avtaler.delete(id)
        .mapLeft { ProcessingError.fromDatabaseOperationError(it) }
}
