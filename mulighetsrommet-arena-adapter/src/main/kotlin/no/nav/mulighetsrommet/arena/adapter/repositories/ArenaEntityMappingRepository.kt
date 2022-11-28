package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

class ArenaEntityMappingRepository(private val db: Database) {

    fun insert(mapping: ArenaEntityMapping): ArenaEntityMapping {
        val column = when (mapping) {
            is ArenaEntityMapping.Tiltakstype -> "tiltakstype_id"
            is ArenaEntityMapping.Tiltaksgjennomforing -> "tiltaksgjennomforing_id"
            is ArenaEntityMapping.Deltaker -> "deltaker_id"
        }

        @Language("PostgreSQL")
        val query = """
            insert into arena_entity_mapping(arena_table, arena_id, $column)
            values (?, ?, ?::uuid)
            returning arena_table, arena_id, tiltakstype_id, tiltaksgjennomforing_id, deltaker_id
        """.trimIndent()

        return queryOf(query, mapping.arenaTable, mapping.arenaId, mapping.entityId)
            .map { it.toMapping() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun get(arenaTable: String, arenaId: String): ArenaEntityMapping? {
        @Language("PostgreSQL")
        val query = """
            select arena_table, arena_id, tiltakstype_id, tiltaksgjennomforing_id, deltaker_id
            from arena_entity_mapping
            where arena_table = ? and arena_id = ?
        """.trimIndent()

        return queryOf(query, arenaTable, arenaId)
            .map { it.toMapping() }
            .asSingle
            .let { db.run(it) }
    }

    private fun Row.toMapping(): ArenaEntityMapping {
        val arenaTable = string("arena_table")
        val arenaId = string("arena_id")

        uuidOrNull("tiltakstype_id")?.let {
            return ArenaEntityMapping.Tiltakstype(arenaTable, arenaId, it)
        }

        uuidOrNull("tiltaksgjennomforing_id")?.let {
            return ArenaEntityMapping.Tiltaksgjennomforing(arenaTable, arenaId, it)
        }

        uuidOrNull("deltaker_id")?.let {
            return ArenaEntityMapping.Deltaker(arenaTable, arenaId, it)
        }

        throw IllegalStateException("ResultSet could not be mapped to ArenaEventToEntity: Relation to entity is missing")
    }
}
