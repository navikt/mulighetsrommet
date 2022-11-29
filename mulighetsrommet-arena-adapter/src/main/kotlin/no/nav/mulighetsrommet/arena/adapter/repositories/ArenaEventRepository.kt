package no.nav.mulighetsrommet.arena.adapter.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ArenaEventRepository(private val db: Database) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun upsert(event: ArenaEvent): ArenaEvent {
        @Language("PostgreSQL")
        val query = """
            insert into arena_events(arena_table, arena_id, payload, consumption_status, message)
            values (:arena_table, :arena_id, :payload::jsonb, :status::consumption_status, :message)
            on conflict (arena_table, arena_id)
            do update set
                payload            = excluded.payload,
                consumption_status = excluded.consumption_status,
                message            = excluded.message
            returning *
        """.trimIndent()

        return queryOf(query, event.toParameters())
            .map { it.toEvent() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun get(table: String, id: String): ArenaEvent? {
        logger.info("Getting event id=$id")

        @Language("PostgreSQL")
        val query = """
            select arena_table, arena_id, payload, consumption_status, message
            from arena_events
            where arena_table = :arena_table and arena_id = :arena_id
        """.trimIndent()

        return queryOf(query, mapOf("arena_table" to table, "arena_id" to id))
            .map { it.toEvent() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(
        table: String? = null,
        status: ArenaEvent.ConsumptionStatus? = null,
        limit: Int = 1000,
        offset: Int = 0
    ): List<ArenaEvent> {
        val where = andWhereParameterNotNull(
            table to "arena_table = :arena_table",
            status to "consumption_status = :status::consumption_status"
        )

        @Language("PostgreSQL")
        val query = """
            select arena_table, arena_id, payload, consumption_status, message
            from arena_events
            $where
            order by arena_id
            limit :limit
            offset :offset
        """.trimIndent()

        return queryOf(
            query,
            mapOf("arena_table" to table, "status" to status?.name, "limit" to limit, "offset" to offset)
        )
            .map { it.toEvent() }
            .asList
            .let { db.run(it) }
    }

    private fun andWhereParameterNotNull(vararg parts: Pair<Any?, String>): String = parts
        .filter { it.first != null }
        .map { it.second }
        .reduceOrNull { where, part -> "$where and $part" }
        ?.let { "where $it" }
        ?: ""

    private fun ArenaEvent.toParameters() = mapOf(
        "arena_table" to arenaTable,
        "arena_id" to arenaId,
        "payload" to payload.toString(),
        "status" to status.name,
        "message" to message
    )

    private fun Row.toEvent() = ArenaEvent(
        arenaTable = string("arena_table"),
        arenaId = string("arena_id"),
        payload = Json.parseToJsonElement(string("payload")),
        status = ArenaEvent.ConsumptionStatus.valueOf(string("consumption_status")),
        message = stringOrNull("message"),
    )
}
