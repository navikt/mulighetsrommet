package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.routes.v1.Status
import no.nav.mulighetsrommet.api.routes.v1.TiltakstypeFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltakstypeRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltakstype: TiltakstypeDbo): QueryResult<TiltakstypeDbo> = query {
        logger.info("Lagrer tiltakstype id=${tiltakstype.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (id, navn, tiltakskode, registrert_dato_i_arena, sist_endret_dato_i_arena, fra_dato, til_dato, rett_paa_tiltakspenger)
            values (:id::uuid, :navn, :tiltakskode, :registrert_dato_i_arena, :sist_endret_dato_i_arena, :fra_dato, :til_dato, :rett_paa_tiltakspenger)
            on conflict (id)
                do update set navn        = excluded.navn,
                              tiltakskode = excluded.tiltakskode,
                              registrert_dato_i_arena = excluded.registrert_dato_i_arena,
                              sist_endret_dato_i_arena = excluded.sist_endret_dato_i_arena,
                              fra_dato = excluded.fra_dato,
                              til_dato = excluded.til_dato,
                              rett_paa_tiltakspenger = excluded.rett_paa_tiltakspenger
            returning *
        """.trimIndent()

        queryOf(query, tiltakstype.toSqlParameters())
            .map { it.toTiltakstypeDbo() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun get(id: UUID): TiltakstypeDto? {
        @Language("PostgreSQL")
        val query = """
            select id::uuid, navn, tiltakskode, registrert_dato_i_arena, sist_endret_dato_i_arena, fra_dato, til_dato, rett_paa_tiltakspenger
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()
        val queryResult = queryOf(query, id).map { it.toTiltakstypeDto() }.asSingle
        return db.run(queryResult)
    }

    fun getAll(
        paginationParams: PaginationParams = PaginationParams()
    ): Pair<Int, List<TiltakstypeDto>> {
        val parameters = mapOf(
            "limit" to paginationParams.limit,
            "offset" to paginationParams.offset
        )

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, registrert_dato_i_arena, sist_endret_dato_i_arena, fra_dato, til_dato, rett_paa_tiltakspenger, count(*) OVER() AS full_count
            from tiltakstype
            order by navn asc
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(query, parameters)
            .map {
                it.int("full_count") to it.toTiltakstypeDto()
            }
            .asList
            .let { db.run(it) }
        val tiltakstyper = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0
        return Pair(totaltAntall, tiltakstyper)
    }

    fun getAll(
        tiltakstypeFilter: TiltakstypeFilter,
        paginationParams: PaginationParams = PaginationParams()
    ): Pair<Int, List<TiltakstypeDto>> {
        val parameters = mapOf(
            "search" to "%${tiltakstypeFilter.search}%",
            "limit" to paginationParams.limit,
            "offset" to paginationParams.offset
        )

        val where = andWhereParameterNotNull(
            tiltakstypeFilter.search to "(lower(navn) like lower(:search))",
            when (tiltakstypeFilter.status) {
                Status.AKTIV -> "" to "(now()::timestamp >= fra_dato and now()::timestamp <= til_dato)"
                Status.PLANLAGT -> "" to "(now()::timestamp < fra_dato)"
                Status.UTFASET -> "" to "(now()::timestamp > til_dato)"
            }
        )

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, registrert_dato_i_arena, sist_endret_dato_i_arena, fra_dato, til_dato, rett_paa_tiltakspenger, count(*) OVER() AS full_count
            from tiltakstype
            $where
            order by navn asc
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(query, parameters)
            .map {
                it.int("full_count") to it.toTiltakstypeDto()
            }
            .asList
            .let { db.run(it) }
        val tiltakstyper = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0
        return Pair(totaltAntall, tiltakstyper)
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter tiltakstype id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    private fun andWhereParameterNotNull(vararg parts: Pair<Any?, String>): String = parts
        .filter { it.first != null }
        .map { it.second }
        .reduceOrNull { where, part -> "$where and $part" }
        ?.let { "where $it" }
        ?: ""

    private fun TiltakstypeDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakskode" to tiltakskode,
        "registrert_dato_i_arena" to registrertIArenaDato,
        "sist_endret_dato_i_arena" to sistEndretIArenaDato,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "rett_paa_tiltakspenger" to rettPaaTiltakspenger
    )

    private fun Row.toTiltakstypeDbo() = TiltakstypeDbo(
        id = uuid("id"),
        navn = string("navn"),
        tiltakskode = string("tiltakskode"),
        registrertIArenaDato = localDateTime("registrert_dato_i_arena"),
        sistEndretIArenaDato = localDateTime("sist_endret_dato_i_arena"),
        fraDato = localDate("fra_dato"),
        tilDato = localDate("til_dato"),
        rettPaaTiltakspenger = boolean("rett_paa_tiltakspenger")
    )

    private fun Row.toTiltakstypeDto() = TiltakstypeDto(
        id = uuid("id"),
        navn = string("navn"),
        arenaKode = string("tiltakskode"),
        fraDato = localDate("fra_dato"),
        tilDato = localDate("til_dato"),
        rettPaaTiltakspenger = boolean("rett_paa_tiltakspenger")
    )
}
