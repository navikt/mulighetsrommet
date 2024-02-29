package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.Utkasttype
import no.nav.mulighetsrommet.api.domain.dto.UtkastDto
import no.nav.mulighetsrommet.api.domain.dto.UtkastRequest
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.api.utils.UtkastFilter
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class UtkastRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(utkast: UtkastRequest): QueryResult<UtkastDto?> = query {
        logger.info("Lagrer utkast id=${utkast.id}")

        @Language("PostgreSQL")
        val query = """
            insert into utkast( id,
                                avtale_id,
                                opprettet_av,
                                utkast_data,
                                utkast_type)
            values (:id::uuid,
                    :avtale_id::uuid,
                    :opprettet_av,
                    :utkast_data::jsonb,
                    :utkast_type::utkasttype)
            on conflict (id) do update set  utkast_data         = excluded.utkast_data,
                                            utkast_type         = excluded.utkast_type
            returning *
        """.trimIndent()

        queryOf(query, utkast.toSqlParams())
            .map { it.toUtkastDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun get(id: UUID): QueryResult<UtkastDto?> = query {
        logger.info("Henter utkast med id: $id")
        @Language("PostgreSQL")
        val query = """
            select * from utkast where id = ?
        """.trimIndent()

        queryOf(query, id)
            .map { it.toUtkastDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(filter: UtkastFilter): QueryResult<List<UtkastDto>> = query {
        logger.info("Henter utkast med filter: $filter")

        val params = mapOf(
            "utkast_type" to filter.type.name,
            "opprettetAv" to filter.opprettetAv?.value,
            "avtaleId" to filter.avtaleId,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.type to "utkast_type = :utkast_type::utkasttype",
            filter.opprettetAv to "opprettet_av = :opprettetAv",
            filter.avtaleId to "avtale_id = :avtaleId::uuid",
        )

        @Language("PostgreSQL")
        val query = """
            select * from utkast $where
            order by updated_at asc
        """.trimIndent()

        queryOf(query, params)
            .map { it.toUtkastDto() }
            .asList
            .let { db.run(it) }
    }

    fun delete(id: UUID) = db.transaction { delete(id, it) }

    fun delete(id: UUID, tx: Session) {
        logger.info("Sletter utkast med id: $id")
        @Language("PostgreSQL")
        val query = """
            delete from utkast where id = ?::uuid
        """.trimIndent()

        tx.run(queryOf(query, id).asUpdate)
    }

    private fun UtkastRequest.toSqlParams() = mapOf(
        "id" to id,
        "avtale_id" to avtaleId,
        "opprettet_av" to opprettetAv.value,
        "utkast_data" to utkastData.toString(),
        "utkast_type" to type.name,
    )

    private fun Row.toUtkastDto(): UtkastDto {
        return UtkastDto(
            id = uuid("id"),
            avtaleId = uuid("avtale_id"),
            opprettetAv = NavIdent(string("opprettet_av")),
            utkastData = Json.decodeFromString(string("utkast_data")),
            createdAt = localDateTime("created_at"),
            updatedAt = localDateTime("updated_at"),
            type = Utkasttype.valueOf(string("utkast_type")),
        )
    }
}
