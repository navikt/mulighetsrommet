package no.nav.mulighetsrommet.arena.adapter.models.db

import java.util.*

data class ArenaEntityMapping(
    val arenaTable: String,
    val arenaId: String,
    val entityId: UUID
)
