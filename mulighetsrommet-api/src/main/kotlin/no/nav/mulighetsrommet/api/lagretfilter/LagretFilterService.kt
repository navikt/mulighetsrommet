package no.nav.mulighetsrommet.api.lagretfilter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import org.intellij.lang.annotations.Language
import java.util.*

class LagretFilterService(private val db: ApiDatabase) {

    fun upsertFilter(brukerId: String, filter: LagretFilter): Either<LagretFilterError, UUID> = db.session {
        checkOwnership(filter.id, brukerId).map {
            @Language("PostgreSQL")
            val query = """
            insert into lagret_filter (id, bruker_id, navn, type, filter, is_default, sort_order)
            values (:id::uuid, :bruker_id, :navn, :type::filter_dokument_type, :filter::jsonb, :is_default, :sort_order)
            on conflict (id) do update set
                navn = excluded.navn,
                filter = excluded.filter,
                sort_order = excluded.sort_order,
                is_default = excluded.is_default
            """.trimIndent()

            val params = mapOf(
                "id" to filter.id,
                "bruker_id" to brukerId,
                "navn" to filter.navn,
                "type" to filter.type.name,
                "filter" to filter.filter.let { Json.encodeToString<JsonElement>(it) },
                "is_default" to filter.isDefault,
                "sort_order" to filter.sortOrder,
            )

            session.execute(queryOf(query, params))

            filter.id
        }
    }

    fun getLagredeFiltereForBruker(
        brukerId: String,
        dokumentType: LagretFilterType,
    ): List<LagretFilter> = db.session {
        @Language("PostgreSQL")
        val query = """
            select *
            from lagret_filter
            where bruker_id = :brukerId and type = :dokumentType::filter_dokument_type
            order by sort_order, created_at
        """.trimIndent()

        val params = mapOf("brukerId" to brukerId, "dokumentType" to dokumentType.name)

        session.list(queryOf(query, params)) {
            LagretFilter(
                id = it.uuid("id"),
                navn = it.string("navn"),
                type = LagretFilterType.valueOf(it.string("type")),
                filter = Json.parseToJsonElement(it.string("filter")),
                isDefault = it.boolean("is_default"),
                sortOrder = it.int("sort_order"),
            )
        }
    }

    fun deleteFilter(brukerId: String, id: UUID): Either<LagretFilterError, UUID?> = db.session {
        checkOwnership(id, brukerId).map {
            @Language("PostgreSQL")
            val query = "delete from lagret_filter where id = ?::uuid"
            val deleted = session.update(queryOf(query, id))
            if (deleted == 0) null else id
        }
    }

    private fun QueryContext.checkOwnership(
        filterId: UUID,
        brukerId: String,
    ): Either<LagretFilterError.Forbidden, UUID> {
        @Language("PostgreSQL")
        val getOwnershipQuery = """
            select bruker_id from lagret_filter where id = ?::uuid
        """.trimIndent()

        val currentOwner = session.single(queryOf(getOwnershipQuery, filterId)) { it.string("bruker_id") }

        return currentOwner?.takeIf { it != brukerId }
            ?.let { LagretFilterError.Forbidden("Du har ikke tilgang til filter med id=$filterId").left() }
            ?: filterId.right()
    }
}
