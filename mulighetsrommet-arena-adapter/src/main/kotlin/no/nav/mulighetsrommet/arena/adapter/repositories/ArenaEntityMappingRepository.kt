package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

class ArenaEntityMappingRepository(private val db: Database) {

    fun insert(mapping: ArenaEntityMapping): ArenaEntityMapping {
        @Language("PostgreSQL")
        val query = """
            insert into arena_entity_mapping(arena_table, arena_id, entity_id)
            values (?, ?, ?::uuid)
            returning arena_table, arena_id, entity_id
        """.trimIndent()

        return queryOf(query, mapping.arenaTable, mapping.arenaId, mapping.entityId)
            .map { it.toMapping() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun get(arenaTable: String, arenaId: String): ArenaEntityMapping? {
        @Language("PostgreSQL")
        val query = """
            select arena_table, arena_id, entity_id
            from arena_entity_mapping
            where arena_table = ? and arena_id = ?
        """.trimIndent()

        return queryOf(query, arenaTable, arenaId)
            .map { it.toMapping() }
            .asSingle
            .let { db.run(it) }
    }

    private fun Row.toMapping(): ArenaEntityMapping {
        return ArenaEntityMapping(
            arenaTable = string("arena_table"),
            arenaId = string("arena_id"),
            entityId = uuid("entity_id")
        )
    }
}
