package no.nav.mulighetsrommet.arena.adapter.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
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
            insert into arena_events(arena_table, arena_id, operation, payload, processing_status, message, retries)
            values (:arena_table, :arena_id, :operation::arena_operation, :payload::jsonb, :status::processing_status, :message, :retries)
            on conflict (arena_table, arena_id)
            do update set
                operation          = excluded.operation,
                payload            = excluded.payload,
                processing_status  = excluded.processing_status,
                message            = excluded.message,
                retries            = excluded.retries
            returning *
        """.trimIndent()

        return queryOf(query, event.toParameters())
            .map { it.toEvent() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun updateProcessingStatusFromEntityStatus(
        table: ArenaTable,
        entityStatus: ArenaEntityMapping.Status,
        processingStatus: ArenaEvent.ProcessingStatus,
    ) {
        @Language("PostgreSQL")
        val query = """
            with mappings as (select arena_table, arena_id
                              from arena_entity_mapping
                              where arena_table = :table
                                and status = :entity_status::entity_status)
            update arena_events e
            set processing_status = :processing_status::processing_status,
                retries           = 0
            from mappings m
            where e.arena_table = m.arena_table
              and e.arena_id = m.arena_id;
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "table" to table.table,
                "entity_status" to entityStatus.name,
                "processing_status" to processingStatus.name,
            ),
        )
            .asUpdate
            .let { db.run(it) }
    }

    fun get(table: ArenaTable, id: String): ArenaEvent? {
        logger.info("Getting event table=$table, id=$id")

        @Language("PostgreSQL")
        val query = """
            select arena_table, arena_id, operation, payload, processing_status, message, retries
            from arena_events
            where arena_table = :arena_table and arena_id = :arena_id
        """.trimIndent()

        return queryOf(query, mapOf("arena_table" to table.table, "arena_id" to id))
            .map { it.toEvent() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(
        table: ArenaTable? = null,
        idGreaterThan: String? = null,
        status: ArenaEvent.ProcessingStatus? = null,
        retriesLessThan: Int? = null,
        retriesGreaterThanOrEqual: Int? = null,
        limit: Int = 1000,
    ): List<ArenaEvent> {
        logger.info("Getting events table=$table, idGreaterThan=$idGreaterThan, status=$status, maxRetries=$retriesLessThan, retriesGreaterThanOrEqual=$retriesGreaterThanOrEqual, limit=$limit")

        val where = andWhereParameterNotNull(
            table to "arena_table = :arena_table",
            idGreaterThan to "arena_id > :arena_id",
            status to "processing_status= :status::processing_status",
            retriesLessThan to "retries < :max_retries",
            retriesGreaterThanOrEqual to "retries >= :retriesGreaterThanOrEqual",
        )

        @Language("PostgreSQL")
        val query = """
            select arena_table, arena_id, operation, payload, processing_status, message, retries
            from arena_events
            $where
            order by arena_id
            limit :limit
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "arena_table" to table?.table,
                "arena_id" to idGreaterThan,
                "status" to status?.name,
                "max_retries" to retriesLessThan,
                "retriesGreaterThanOrEqual" to retriesGreaterThanOrEqual,
                "limit" to limit,
            ),
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
        "arena_table" to arenaTable.table,
        "arena_id" to arenaId,
        "operation" to operation.name,
        "payload" to payload.toString(),
        "status" to status.name,
        "message" to message,
        "retries" to retries,
    )

    private fun Row.toEvent() = ArenaEvent(
        arenaTable = ArenaTable.fromTable(string("arena_table")),
        arenaId = string("arena_id"),
        operation = ArenaEvent.Operation.valueOf(string("operation")),
        payload = Json.parseToJsonElement(string("payload")),
        status = ArenaEvent.ProcessingStatus.valueOf(string("processing_status")),
        message = stringOrNull("message"),
        retries = int("retries"),
    )
}
