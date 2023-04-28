package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
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
        paginationParams: PaginationParams = PaginationParams(),
    ): Pair<Int, List<TiltakstypeDto>> {
        val parameters = mapOf(
            "limit" to paginationParams.limit,
            "offset" to paginationParams.offset,
        )

        @Language("PostgreSQL")
        val query = """
            select
                id,
                navn,
                tiltakskode,
                registrert_dato_i_arena,
                sist_endret_dato_i_arena,
                fra_dato,
                til_dato,
                rett_paa_tiltakspenger,
                count(*) OVER() AS full_count
            from tiltakstype
            order by navn asc
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(query, parameters)
            .map { it.int("full_count") to it.toTiltakstypeDto() }
            .asList
            .let { db.run(it) }
        val tiltakstyper = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0
        return Pair(totaltAntall, tiltakstyper)
    }

    fun getAllSkalMigreres(
        tiltakstypeFilter: TiltakstypeFilter,
        paginationParams: PaginationParams = PaginationParams(),
    ): Pair<Int, List<TiltakstypeDto>> {
        val parameters = mapOf(
            "search" to "%${tiltakstypeFilter.search}%",
            "limit" to paginationParams.limit,
            "offset" to paginationParams.offset,
            "gruppetiltakskoder" to db.createTextArray(Tiltakskoder.Gruppetiltak),
            "today" to tiltakstypeFilter.dagensDato,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            tiltakstypeFilter.search to "(lower(navn) like lower(:search))",
            tiltakstypeFilter.status to tiltakstypeFilter.status?.toDbStatement(),
            tiltakstypeFilter.kategori to tiltakstypeFilter.kategori?.let {
                when (it) {
                    Tiltakstypekategori.GRUPPE -> "tiltakskode = any(:gruppetiltakskoder)"
                    Tiltakstypekategori.INDIVIDUELL -> "not(tiltakskode = any(:gruppetiltakskoder))"
                }
            },
            true to "skal_migreres = true",
        )

        val order = when (tiltakstypeFilter.sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "startdato-ascending" -> "fra_dato asc"
            "startdato-descending" -> "fra_dato desc"
            "sluttdato-ascending" -> "til_dato asc"
            "sluttdato-descending" -> "til_dato desc"
            else -> "navn asc"
        }

        @Language("PostgreSQL")
        val query = """
            select
                id,
                navn,
                tiltakskode,
                registrert_dato_i_arena,
                sist_endret_dato_i_arena,
                fra_dato,
                til_dato,
                rett_paa_tiltakspenger,
                count(*) OVER() AS full_count
            from tiltakstype
            $where
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(query, parameters)
            .map { it.int("full_count") to it.toTiltakstypeDto() }
            .asList
            .let { db.run(it) }
        val tiltakstyper = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0
        return Pair(totaltAntall, tiltakstyper)
    }

    fun getAllByDateInterval(
        dateIntervalStart: LocalDate,
        dateIntervalEnd: LocalDate,
        pagination: PaginationParams,
    ): List<TiltakstypeDto> {
        logger.info("Henter alle tiltakstyper med start- eller sluttdato mellom $dateIntervalStart og $dateIntervalEnd")

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, registrert_dato_i_arena, sist_endret_dato_i_arena, fra_dato, til_dato, rett_paa_tiltakspenger, count(*) OVER() AS full_count
            from tiltakstype
            where
                (fra_dato > :date_interval_start and fra_dato <= :date_interval_end) or
                (til_dato >= :date_interval_start and til_dato < :date_interval_end)
            order by navn
            limit :limit offset :offset
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "date_interval_start" to dateIntervalStart,
                "date_interval_end" to dateIntervalEnd,
                "limit" to pagination.limit,
                "offset" to pagination.offset,
            ),
        )
            .map { it.toTiltakstypeDto() }
            .asList
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter tiltakstype id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    private fun TiltakstypeDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakskode" to tiltakskode,
        "registrert_dato_i_arena" to registrertDatoIArena,
        "sist_endret_dato_i_arena" to sistEndretDatoIArena,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "rett_paa_tiltakspenger" to rettPaaTiltakspenger,
    )

    private fun Row.toTiltakstypeDbo() = TiltakstypeDbo(
        id = uuid("id"),
        navn = string("navn"),
        tiltakskode = string("tiltakskode"),
        registrertDatoIArena = localDateTime("registrert_dato_i_arena"),
        sistEndretDatoIArena = localDateTime("sist_endret_dato_i_arena"),
        fraDato = localDate("fra_dato"),
        tilDato = localDate("til_dato"),
        rettPaaTiltakspenger = boolean("rett_paa_tiltakspenger"),
    )

    private fun Row.toTiltakstypeDto(): TiltakstypeDto {
        val fraDato = localDate("fra_dato")
        val tilDato = localDate("til_dato")
        return TiltakstypeDto(
            id = uuid("id"),
            navn = string("navn"),
            arenaKode = string("tiltakskode"),
            registrertIArenaDato = localDateTime("registrert_dato_i_arena"),
            sistEndretIArenaDato = localDateTime("sist_endret_dato_i_arena"),
            fraDato = fraDato,
            tilDato = tilDato,
            rettPaaTiltakspenger = boolean("rett_paa_tiltakspenger"),
            status = Tiltakstypestatus.resolveFromDates(LocalDate.now(), fraDato, tilDato),
        )
    }

    private fun Tiltakstypestatus.toDbStatement(): String {
        return when (this) {
            Tiltakstypestatus.Planlagt -> "(:today < fra_dato)"
            Tiltakstypestatus.Aktiv -> "(:today >= fra_dato and :today <= til_dato)"
            else -> "(:today > til_dato)"
        }
    }
}
