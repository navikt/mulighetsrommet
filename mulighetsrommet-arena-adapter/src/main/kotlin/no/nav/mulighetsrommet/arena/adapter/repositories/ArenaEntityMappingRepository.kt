package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

class ArenaEntityMappingRepository(private val db: Database) {

    fun insert(mapping: ArenaEntityMapping): ArenaEntityMapping {
        val column = getEntityIdColumn(mapping.arenaTable)

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

    private fun getEntityIdColumn(arenaTable: String) = when (arenaTable) {
        ArenaTables.Tiltakstype -> "tiltakstype_id"
        ArenaTables.Tiltaksgjennomforing -> "tiltaksgjennomforing_id"
        ArenaTables.Deltaker -> "deltaker_id"
        else -> throw IllegalStateException("The table '$arenaTable' is not mapped to an Arena entity")
    }

    private fun Row.toMapping(): ArenaEntityMapping {
        val table = string("arena_table")
        return ArenaEntityMapping(
            arenaTable = table,
            arenaId = string("arena_id"),
            entityId = uuid(getEntityIdColumn(table))
        )
    }
}
