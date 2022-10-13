package no.nav.mulighetsrommet.arena.adapter.models.db

import java.util.*

sealed class ArenaEntityMapping(val arenaTable: String, val arenaId: String, val entityId: UUID) {
    class Tiltakstype(arenaTable: String, arenaId: String, tiltakstypeId: UUID) :
        ArenaEntityMapping(arenaTable, arenaId, tiltakstypeId)

    class Tiltaksgjennomforing(arenaTable: String, arenaId: String, tiltaksgjennomforingId: UUID) :
        ArenaEntityMapping(arenaTable, arenaId, tiltaksgjennomforingId)

    class Deltaker(arenaTable: String, arenaId: String, deltakerId: UUID) :
        ArenaEntityMapping(arenaTable, arenaId, deltakerId)
}
