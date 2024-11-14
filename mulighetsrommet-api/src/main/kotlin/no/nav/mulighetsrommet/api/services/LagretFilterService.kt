package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class LagretFilterService(private val db: Database) {

    fun upsertFilter(filter: UpsertFilterEntry): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            insert into lagret_filter (id, bruker_id, navn, type, filter, sort_order, sist_brukt)
            values (:id::uuid, :brukerId, :navn, :type::filter_dokument_type, :filter::jsonb, :sortOrder, :sistBrukt)
            on conflict (id) do update set
                navn = excluded.navn,
                filter = excluded.filter,
                sort_order = excluded.sort_order,
                sist_brukt = excluded.sist_brukt
        """.trimIndent()

        queryOf(query, filter.toSqlParams()).asExecute.let { db.run(it) }
    }

    fun getLagredeFiltereForBruker(
        brukerId: String,
        dokumentType: UpsertFilterEntry.FilterDokumentType,
    ): QueryResult<List<LagretFilter>> = query {
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
                    sistBrukt = it.localDateTimeOrNull("sist_brukt"),
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

    fun updateSistBruktTimestamp(id: UUID, sistBruktTidspunkt: LocalDateTime = LocalDateTime.now()) {
        @Language("PostgreSQL")
        val query = """
            update lagret_filter
            set sist_brukt = :sistBrukt
            where id = :id::uuid
        """.trimIndent()

        queryOf(query, mapOf("id" to id, "sistBrukt" to sistBruktTidspunkt)).asExecute.let { db.run(it) }
    }

    fun clearSistBruktTimestampForBruker(brukerId: String, dokumentType: UpsertFilterEntry.FilterDokumentType) {
        @Language("PostgreSQL")
        val query = """
            update lagret_filter
            set sist_brukt = null
            where bruker_id = :brukerId and type = :dokumentType::filter_dokument_type
        """.trimIndent()

        queryOf(query, mapOf("brukerId" to brukerId, "dokumentType" to dokumentType.name)).asExecute.let { db.run(it) }
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
        "sistBrukt" to sistBrukt,
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
    @Serializable(with = LocalDateTimeSerializer::class)
    val sistBrukt: LocalDateTime?,
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
    @Serializable(with = LocalDateTimeSerializer::class)
    val sistBrukt: LocalDateTime?,
)
