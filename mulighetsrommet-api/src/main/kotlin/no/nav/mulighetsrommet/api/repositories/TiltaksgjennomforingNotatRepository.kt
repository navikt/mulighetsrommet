package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingNotatDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNotatDto
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dto.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingNotatRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(notat: TiltaksgjennomforingNotatDbo): QueryResult<Unit> = query {
        logger.info("Lagrer notat for tiltaksgjennomforing id=${notat.id}, tiltaksgjennomforingId: ${notat.tiltaksgjennomforingId}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing_notat(id,
                               tiltaksgjennomforing_id,
                               opprettet_av,
                               innhold
                               )
            values (:id::uuid,
                    :tiltaksgjennomforingId::uuid,
                    :opprettet_av,
                    :innhold)
            on conflict (id) do update set innhold = excluded.innhold
            returning *
        """.trimIndent()

        queryOf(query, notat.toSqlParameters()).asExecute.let { db.run(it) }
    }

    fun get(id: UUID): QueryResult<TiltaksgjennomforingNotatDto?> = query {
        @Language("PostgreSQL")
        val query = """
            select id,
            tiltaksgjennomforing_id,
            created_at,
            updated_at,
            innhold,
            jsonb_build_object('navIdent', na.nav_ident, 'navn', concat(na.fornavn, ' ', na.etternavn)) as opprettetAv
            from tiltaksgjennomforing_notat tn join nav_ansatt na on tn.opprettet_av = na.nav_ident
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .map { it.toTiltaksgjennomforingNotatDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter notat for tiltaksgjennomføring med id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing_notat
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    fun getAll(
        filter: NotatFilter,
    ): QueryResult<List<TiltaksgjennomforingNotatDto>> = query {
        val parameters = mapOf(
            "tiltaksgjennomforingId" to filter.tiltaksgjennomforingId,
            "opprettetAv" to filter.opprettetAv?.value,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.tiltaksgjennomforingId to "tiltaksgjennomforing_id = :tiltaksgjennomforingId::uuid",
            filter.opprettetAv to "opprettet_av = :opprettetAv",
        )

        val order = when (filter.sortering) {
            "dato-sortering-asc" -> "created_at asc"
            "dato-sortering-desc" -> "created_at desc"
            else -> "created_at asc"
        }

        @Language("PostgreSQL")
        val query = """
            select id,
            tiltaksgjennomforing_id,
            created_at,
            updated_at,
            innhold,
            jsonb_build_object('navIdent', na.nav_ident, 'navn', concat(na.fornavn, ' ', na.etternavn)) as opprettetAv
            from tiltaksgjennomforing_notat tn join nav_ansatt na on tn.opprettet_av = na.nav_ident
            $where
            order by $order
        """.trimIndent()

        queryOf(query, parameters)
            .map {
                it.toTiltaksgjennomforingNotatDto()
            }
            .asList
            .let { db.run(it) }
    }

    private fun TiltaksgjennomforingNotatDbo.toSqlParameters() = mapOf(
        "id" to id,
        "tiltaksgjennomforingId" to tiltaksgjennomforingId,
        "opprettet_av" to opprettetAv?.value,
        "innhold" to innhold,
    )

    private fun Row.toTiltaksgjennomforingNotatDto(): TiltaksgjennomforingNotatDto {
        val opprettetAv = string("opprettetAv").let {
            Json.decodeFromString<TiltaksgjennomforingNotatDto.OpprettetAv>(it)
        }

        return TiltaksgjennomforingNotatDto(
            id = uuid("id"),
            tiltaksgjennomforingId = uuid("tiltaksgjennomforing_id"),
            createdAt = localDateTime("created_at"),
            updatedAt = localDateTime("updated_at"),
            opprettetAv = opprettetAv,
            innhold = string("innhold"),
        )
    }
}
