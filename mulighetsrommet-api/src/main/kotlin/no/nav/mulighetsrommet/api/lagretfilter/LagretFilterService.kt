package no.nav.mulighetsrommet.api.lagretfilter

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import org.intellij.lang.annotations.Language
import java.util.*

class LagretFilterService(private val db: ApiDatabase) {

    // TODO: access control
    fun upsertFilter(filter: LagretFilterUpsert): Unit = db.session {
        @Language("PostgreSQL")
        val query = """
            insert into lagret_filter (id, bruker_id, navn, type, filter, sort_order)
            values (:id::uuid, :brukerId, :navn, :type::filter_dokument_type, :filter::jsonb, :sortOrder)
            on conflict (id) do update set
                navn = excluded.navn,
                filter = excluded.filter,
                sort_order = excluded.sort_order
        """.trimIndent()

        val params = mapOf(
            "id" to filter.id,
            "brukerId" to filter.brukerId,
            "navn" to filter.navn,
            "type" to filter.type.name,
            "filter" to filter.filter.let { Json.encodeToString<JsonElement>(it) },
            "sortOrder" to filter.sortOrder,
        )

        session.execute(queryOf(query, params))
    }

    // TODO: access control
    fun getLagredeFiltereForBruker(
        brukerId: String,
        dokumentType: FilterDokumentType,
    ): List<LagretFilterDto> = db.session {
        @Language("PostgreSQL")
        val query = """
            select * from lagret_filter
            where bruker_id = :brukerId and type = :dokumentType::filter_dokument_type
            order by sort_order, created_at
        """.trimIndent()

        val params = mapOf("brukerId" to brukerId, "dokumentType" to dokumentType.name)

        session.list(queryOf(query, params)) {
            LagretFilterDto(
                id = it.uuid("id"),
                brukerId = it.string("bruker_id"),
                navn = it.string("navn"),
                type = FilterDokumentType.valueOf(it.string("type")),
                filter = Json.parseToJsonElement(it.string("filter")),
                sortOrder = it.int("sort_order"),
            )
        }
    }

    fun deleteFilter(id: UUID): Unit = db.session {
        @Language("PostgreSQL")
        val query = "delete from lagret_filter where id = ?::uuid"

        session.execute(queryOf(query, id))
    }
}
