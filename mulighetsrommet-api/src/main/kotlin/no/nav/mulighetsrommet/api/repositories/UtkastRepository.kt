package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.UtkastDbo
import no.nav.mulighetsrommet.api.domain.dbo.Utkasttype
import no.nav.mulighetsrommet.api.domain.dto.UtkastDto
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dto.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class UtkastRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(utkast: UtkastDbo): QueryResult<UtkastDto?> = query {
        logger.info("Lagrer utkast id=${utkast.id}")

        @Language("PostgreSQL")
        val query = """
            insert into utkast(id,
                               opprettet_av,
                               utkast_data,
                               utkast_type)
            values (:id::uuid,
                    :opprettet_av,
                    :utkast_data::jsonb,
                    :utkast_type::utkasttype)
            on conflict (id) do update set  opprettet_av        = excluded.opprettet_av,
                                            utkast_data         = excluded.utkast_data,
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

    fun delete(id: UUID) = query {
        logger.info("Sletter utkast med id: $id")
        @Language("PostgreSQL")
        val query = """
            delete from utkast where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    private fun UtkastDbo.toSqlParams() = mapOf(
        "id" to id,
        "opprettet_av" to opprettetAv,
        "utkast_data" to utkastData.toString(),
        "utkast_type" to type.name,
    )

    private fun Row.toUtkastDto(): UtkastDto {
        return UtkastDto(
            id = uuid("id"),
            opprettetAv = string("opprettet_av"),
            utkastData = Json.decodeFromString(string("utkast_data")),
            createdAt = localDateTime("created_at"),
            updatedAt = localDateTime("updated_at"),
            type = Utkasttype.valueOf(string("utkast_type")),
        )
    }
}
