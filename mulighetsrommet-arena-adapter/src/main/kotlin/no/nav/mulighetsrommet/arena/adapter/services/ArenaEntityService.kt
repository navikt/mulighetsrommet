package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.Either
import arrow.core.rightIfNotNull
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.db.*
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import java.util.*

class ArenaEntityService(
    private val events: ArenaEventRepository,
    private val mappings: ArenaEntityMappingRepository,
    private val tiltakstyper: TiltakstypeRepository,
    private val saker: SakRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val deltakere: DeltakerRepository
) {

    fun getEvent(arenaTable: String, arenaId: String): Either<ConsumptionError, ArenaEvent> {
        return events.get(arenaTable, arenaId)
            .rightIfNotNull { ConsumptionError.MissingDependency("ArenaEntityMapping mangler for arenaTable=$arenaTable og arenaId=$arenaId") }
    }

    fun getOrCreateMapping(event: ArenaEvent): ArenaEntityMapping {
        return mappings.get(event.arenaTable, event.arenaId)
            ?: mappings.insert(ArenaEntityMapping(event.arenaTable, event.arenaId, UUID.randomUUID()))
    }

    fun getMapping(arenaTable: String, arenaId: String): Either<ConsumptionError, ArenaEntityMapping> {
        return mappings.get(arenaTable, arenaId)
            .rightIfNotNull { ConsumptionError.MissingDependency("ArenaEntityMapping mangler for arenaTable=$arenaTable og arenaId=$arenaId") }
    }

    fun upsertTiltakstype(tiltakstype: Tiltakstype): Either<ConsumptionError, Tiltakstype> {
        return tiltakstyper.upsert(tiltakstype)
            .mapLeft { ConsumptionError.fromDatabaseOperationError(it) }
    }

    fun upsertSak(sak: Sak): Either<ConsumptionError, Sak> {
        return saker.upsert(sak)
            .mapLeft { ConsumptionError.fromDatabaseOperationError(it) }
    }

    fun getSak(id: Int): Either<ConsumptionError.MissingDependency, Sak> {
        return saker.get(id)
            .rightIfNotNull { ConsumptionError.MissingDependency("Sak med id=$id mangler") }
    }

    fun upsertTiltaksgjennomforing(tiltaksgjennomforing: Tiltaksgjennomforing): Either<ConsumptionError, Tiltaksgjennomforing> {
        return tiltaksgjennomforinger.upsert(tiltaksgjennomforing)
            .mapLeft { ConsumptionError.fromDatabaseOperationError(it) }
    }

    fun isIgnored(arenaTable: String, arenaId: String): Either<ConsumptionError, Boolean> {
        // TODO: burde status Ignored settes på ArenaEntityMapping i stedet?
        //       Da har vi mulighet til å slette data fra events-tabellen, samtidig som vi har oversikt over hvilke entitier som ikke er relevante
        return getEvent(arenaTable, arenaId)
            .map { it.status == ArenaEvent.ConsumptionStatus.Ignored }
    }

    fun upsertDeltaker(deltaker: Deltaker): Either<ConsumptionError, Deltaker> {
        return deltakere.upsert(deltaker)
            .mapLeft { ConsumptionError.fromDatabaseOperationError(it) }
    }
}
