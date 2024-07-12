package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.util.*

class LagretFilterService(private val db: Database) {

    fun upsertFilter(filter: UpsertFilterEntry): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            insert into lagret_filter (id, bruker_id, navn, type, filter, sort_order)
            values (:id::uuid, :brukerId, :navn, :type::filter_dokument_type, :filter::jsonb, :sortOrder)
            on conflict (id) do update set
                navn = excluded.navn,
                filter = excluded.filter,
                sort_order = excluded.sort_order
        """.trimIndent()

        queryOf(query, filter.toSqlParams()).asExecute.let { db.run(it) }
    }

    fun getLagredeFiltereForBruker(brukerId: String, dokumentType: UpsertFilterEntry.FilterDokumentType): QueryResult<List<LagretFilter>> = query {
        @Language("PostgreSQL")
        val query = """
            select * from lagret_filter
            where bruker_id = :brukerId and type = :dokumentType::filter_dokument_type
            order by sort_order, created_at
        """.trimIndent()

        queryOf(query, mapOf("brukerId" to brukerId, "dokumentType" to dokumentType.name))
            .map { it ->
                LagretFilter(
                    id = it.string("id").let { UUID.fromString(it) },
                    brukerId = it.string("bruker_id"),
                    navn = it.string("navn"),
                    type = UpsertFilterEntry.FilterDokumentType.valueOf(it.string("type")),
                    filter = Json.parseToJsonElement(it.string("filter")),
                    sortOrder = it.int("sort_order"),
                )
            }
            .asList
            .let { db.run(it) }
    }

    fun deleteFilter(id: UUID): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = "delete from lagret_filter where id = :id::uuid"

        queryOf(query, mapOf("id" to id)).asExecute.let { db.run(it) }
    }
}

private fun UpsertFilterEntry.toSqlParams(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "brukerId" to brukerId,
        "navn" to navn,
        "type" to type.name,
        "filter" to filter.let { Json.encodeToString(it) },
        "sortOrder" to sortOrder,
    )
}

@Serializable
data class UpsertFilterEntry(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = UUID.randomUUID(),
    val brukerId: String,
    val navn: String,
    val type: FilterDokumentType,
    val filter: JsonElement,
    val sortOrder: Int,
) {
    enum class FilterDokumentType {
        Avtale,
        Tiltaksgjennomføring,

        @Suppress("ktlint:standard:enum-entry-name-case")
        Tiltaksgjennomføring_Modia,
    }
}

@Serializable
data class LagretFilter(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val brukerId: String,
    val navn: String,
    val type: UpsertFilterEntry.FilterDokumentType,
    val filter: JsonElement,
    val sortOrder: Int,
)
