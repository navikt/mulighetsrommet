package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

class ArenaEntityMappingRepository(private val db: Database) {

    fun upsert(mapping: ArenaEntityMapping): ArenaEntityMapping {
        @Language("PostgreSQL")
        val query = """
            insert into arena_entity_mapping(arena_table, arena_id, entity_id, status, message)
            values (?, ?, ?::uuid, ?::entity_status, ?)
            on conflict (arena_table, arena_id)
            do update set
                entity_id          = excluded.entity_id,
                status             = excluded.status,
                message            = excluded.message
            returning arena_table, arena_id, entity_id, status, message
        """.trimIndent()

        return queryOf(query, mapping.arenaTable.table, mapping.arenaId, mapping.entityId, mapping.status.name, mapping.message)
            .map { it.toMapping() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun get(arenaTable: ArenaTable, arenaId: String): ArenaEntityMapping? {
        @Language("PostgreSQL")
        val query = """
            select arena_table, arena_id, entity_id, status, message
            from arena_entity_mapping
            where arena_table = ? and arena_id = ?
        """.trimIndent()

        return queryOf(query, arenaTable.table, arenaId)
            .map { it.toMapping() }
            .asSingle
            .let { db.run(it) }
    }

    private fun Row.toMapping(): ArenaEntityMapping {
        return ArenaEntityMapping(
            arenaTable = ArenaTable.fromTable(string("arena_table")),
            arenaId = string("arena_id"),
            entityId = uuid("entity_id"),
            status = ArenaEntityMapping.Status.valueOf(string("status")),
            message = stringOrNull("message"),
        )
    }
}
