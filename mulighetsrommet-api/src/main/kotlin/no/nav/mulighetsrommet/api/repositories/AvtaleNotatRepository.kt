package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNotatDto
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dto.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class AvtaleNotatRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(avtaleNotat: AvtaleNotatDbo): QueryResult<Unit> = query {
        logger.info("Lagrer notat for avtale id=${avtaleNotat.id}, avtaleId: ${avtaleNotat.avtaleId}")

        @Language("PostgreSQL")
        val query = """
            insert into avtale_notat(id,
                               avtale_id,
                               opprettet_av,
                               innhold
                               )
            values (:id::uuid,
                    :avtaleId::uuid,
                    :opprettet_av,
                    :innhold)
            on conflict (id) do update set innhold = excluded.innhold
            returning *
        """.trimIndent()

        queryOf(query, avtaleNotat.toSqlParameters()).asExecute.let { db.run(it) }
    }

    fun get(id: UUID): QueryResult<AvtaleNotatDto?> = query {
        @Language("PostgreSQL")
        val query = """
            select id,
            avtale_id,
            created_at,
            updated_at,
            innhold,
            jsonb_build_object('navIdent', na.nav_ident, 'navn', concat(na.fornavn, ' ', na.etternavn)) as opprettetAv
            from avtale_notat an join nav_ansatt na on an.opprettet_av = na.nav_ident
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .map { it.toAvtaleNotatDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter notat for avtale med id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from avtale_notat
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    fun getAll(
        filter: NotatFilter,
    ): QueryResult<List<AvtaleNotatDto>> = query {
        val parameters = mapOf(
            "avtaleId" to filter.avtaleId,
            "opprettetAv" to filter.opprettetAv?.value,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.avtaleId to "avtale_id = :avtaleId::uuid",
            filter.opprettetAv to "opprettet_av = :opprettetAv",
        )

        val order = when (filter.sortering) {
            "dato-sortering-asc" -> "created_at asc"
            "dato-sortering-desc" -> "created_at desc"
            else -> "created_at desc"
        }

        @Language("PostgreSQL")
        val query = """
            select id,
            avtale_id,
            created_at,
            updated_at,
            innhold,
            jsonb_build_object('navIdent', na.nav_ident, 'navn', concat(na.fornavn, ' ', na.etternavn)) as opprettetAv
            from avtale_notat an join nav_ansatt na on an.opprettet_av = na.nav_ident
            $where
            order by $order
        """.trimIndent()

        queryOf(query, parameters)
            .map {
                it.toAvtaleNotatDto()
            }
            .asList
            .let { db.run(it) }
    }

    private fun AvtaleNotatDbo.toSqlParameters() = mapOf(
        "id" to id,
        "avtaleId" to avtaleId,
        "opprettet_av" to opprettetAv?.value,
        "innhold" to innhold,
    )

    private fun Row.toAvtaleNotatDto(): AvtaleNotatDto {
        val opprettetAv = string("opprettetAv").let {
            Json.decodeFromString<AvtaleNotatDto.OpprettetAv>(it)
        }

        return AvtaleNotatDto(
            id = uuid("id"),
            avtaleId = uuid("avtale_id"),
            createdAt = localDateTime("created_at"),
            updatedAt = localDateTime("updated_at"),
            opprettetAv = opprettetAv,
            innhold = string("innhold"),
        )
    }
}
