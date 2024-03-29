package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.api.domain.dto.Innholdselement
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeEksternDto
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltakstypeAdminDto
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class TiltakstypeRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltakstype: TiltakstypeDbo): TiltakstypeDbo {
        logger.info("Lagrer tiltakstype id=${tiltakstype.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (
                id,
                navn,
                tiltakskode,
                arena_kode,
                registrert_dato_i_arena,
                sist_endret_dato_i_arena,
                fra_dato,
                til_dato,
                rett_paa_tiltakspenger
            )
            values (
                :id::uuid,
                :navn,
                :tiltakskode::tiltakskode,
                :arena_kode,
                :registrert_dato_i_arena,
                :sist_endret_dato_i_arena,
                :fra_dato,
                :til_dato,
                :rett_paa_tiltakspenger
            )
            on conflict (id)
                do update set navn        = excluded.navn,
                              tiltakskode = excluded.tiltakskode,
                              arena_kode = excluded.arena_kode,
                              registrert_dato_i_arena = excluded.registrert_dato_i_arena,
                              sist_endret_dato_i_arena = excluded.sist_endret_dato_i_arena,
                              fra_dato = excluded.fra_dato,
                              til_dato = excluded.til_dato,
                              rett_paa_tiltakspenger = excluded.rett_paa_tiltakspenger
            returning *
        """.trimIndent()

        return queryOf(query, tiltakstype.toSqlParameters()).map { it.toTiltakstypeDbo() }.asSingle.let { db.run(it)!! }
    }

    fun get(id: UUID): TiltakstypeAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select
                id::uuid,
                navn,
                arena_kode,
                registrert_dato_i_arena,
                sist_endret_dato_i_arena,
                fra_dato,
                til_dato,
                rett_paa_tiltakspenger,
                sanity_id
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()
        val queryResult = queryOf(query, id).map { it.toTiltakstypeAdminDto() }.asSingle
        return db.run(queryResult)
    }

    fun getEksternTiltakstype(id: UUID): TiltakstypeEksternDto? {
        val tiltakstype = get(id) ?: return null
        val tiltakskode = Tiltakskode.fromArenaKode(tiltakstype.arenaKode) ?: return null
        val deltakerRegistreringInnhold = getDeltakerregistreringInnholdByArenaKode(tiltakskode)

        return TiltakstypeEksternDto(
            id = tiltakstype.id,
            navn = tiltakstype.navn,
            tiltakskode = tiltakskode,
            arenaKode = tiltakstype.arenaKode,
            registrertIArenaDato = tiltakstype.registrertIArenaDato,
            sistEndretIArenaDato = tiltakstype.sistEndretIArenaDato,
            fraDato = tiltakstype.fraDato,
            tilDato = tiltakstype.tilDato,
            rettPaaTiltakspenger = tiltakstype.rettPaaTiltakspenger,
            status = tiltakstype.status,
            deltakerRegistreringInnhold = deltakerRegistreringInnhold,
        )
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select
                id::uuid,
                navn,
                arena_kode,
                registrert_dato_i_arena,
                sist_endret_dato_i_arena,
                fra_dato,
                til_dato,
                rett_paa_tiltakspenger,
                sanity_id
            from tiltakstype
            where sanity_id = ?::uuid
        """.trimIndent()
        val queryResult = queryOf(query, sanityId).map { it.toTiltakstypeAdminDto() }.asSingle
        return db.run(queryResult)
    }

    fun getAll(
        paginationParams: PaginationParams = PaginationParams(),
    ): Pair<Int, List<TiltakstypeAdminDto>> {
        val parameters = mapOf(
            "limit" to paginationParams.limit,
            "offset" to paginationParams.offset,
        )

        @Language("PostgreSQL")
        val query = """
            select
                id,
                navn,
                arena_kode,
                registrert_dato_i_arena,
                sist_endret_dato_i_arena,
                fra_dato,
                til_dato,
                sanity_id,
                rett_paa_tiltakspenger,
                count(*) OVER() AS full_count
            from tiltakstype
            order by navn asc
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(
            query,
            parameters,
        ).map { it.int("full_count") to it.toTiltakstypeAdminDto() }.asList.let { db.run(it) }
        val tiltakstyper = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0
        return Pair(totaltAntall, tiltakstyper)
    }

    fun getAllSkalMigreres(
        pagination: PaginationParams = PaginationParams(),
        dagensDato: LocalDate = LocalDate.now(),
        statuser: List<Tiltakstypestatus> = emptyList(),
        sortering: String? = null,
    ): Pair<Int, List<TiltakstypeAdminDto>> {
        val parameters = mapOf(
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "today" to dagensDato,
        )

        val order = when (sortering) {
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
                arena_kode,
                registrert_dato_i_arena,
                sist_endret_dato_i_arena,
                fra_dato,
                til_dato,
                sanity_id,
                rett_paa_tiltakspenger,
                count(*) OVER() AS full_count
            from tiltakstype
            where tiltakskode is not null
              and ${statuserWhereStatement(statuser)}
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(
            query,
            parameters,
        ).map { it.int("full_count") to it.toTiltakstypeAdminDto() }.asList.let { db.run(it) }
        val tiltakstyper = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0
        return Pair(totaltAntall, tiltakstyper)
    }

    private fun getDeltakerregistreringInnholdByArenaKode(tiltakskode: Tiltakskode): DeltakerRegistreringInnholdDto? {
        @Language("PostgreSQL")
        val query = """
           select dri.innholdskode, tekst, tt.deltaker_registrering_ledetekst
           from tiltakstype tt
               left join tiltakstype_deltaker_registrering_innholdselement tdri on tdri.tiltakskode = tt.tiltakskode
               left join deltaker_registrering_innholdselement dri on tdri.innholdskode = dri.innholdskode
           where tt.tiltakskode = ?::tiltakskode and tt.deltaker_registrering_ledetekst is not null;
        """.trimIndent()

        val result = queryOf(query, tiltakskode.name)
            .map {
                val innholdskode = it.stringOrNull("innholdskode")
                val tekst = it.stringOrNull("tekst")
                val ledetekst = it.string("deltaker_registrering_ledetekst")
                val innholdselement = if (tekst != null && innholdskode != null) {
                    Innholdselement(
                        tekst = tekst,
                        innholdskode = innholdskode,
                    )
                } else {
                    null
                }
                Pair(
                    ledetekst,
                    innholdselement,
                )
            }
            .asList
            .let { db.run(it) }

        if (result.isEmpty()) return null

        val innholdselementer = result.mapNotNull { it.second }
        return DeltakerRegistreringInnholdDto(
            ledetekst = result[0].first,
            innholdselementer = innholdselementer,
        )
    }

    fun getAllByDateInterval(
        dateIntervalStart: LocalDate,
        dateIntervalEnd: LocalDate,
        pagination: PaginationParams,
    ): List<TiltakstypeAdminDto> {
        logger.info("Henter alle tiltakstyper med start- eller sluttdato mellom $dateIntervalStart og $dateIntervalEnd")

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, arena_kode, sanity_id, registrert_dato_i_arena, sist_endret_dato_i_arena, fra_dato, til_dato, rett_paa_tiltakspenger, deltaker_registrering_ledetekst, count(*) OVER() AS full_count
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
        ).map { it.toTiltakstypeAdminDto() }.asList.let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter tiltakstype id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id).asUpdate.let { db.run(it) }
    }

    private fun TiltakstypeDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakskode" to Tiltakskode.fromArenaKode(arenaKode)?.name,
        "arena_kode" to arenaKode,
        "registrert_dato_i_arena" to registrertDatoIArena,
        "sist_endret_dato_i_arena" to sistEndretDatoIArena,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "rett_paa_tiltakspenger" to rettPaaTiltakspenger,
    )

    private fun Row.toTiltakstypeDbo(): TiltakstypeDbo {
        return TiltakstypeDbo(
            id = uuid("id"),
            navn = string("navn"),
            arenaKode = string("arena_kode"),
            registrertDatoIArena = localDateTime("registrert_dato_i_arena"),
            sistEndretDatoIArena = localDateTime("sist_endret_dato_i_arena"),
            fraDato = localDate("fra_dato"),
            tilDato = localDate("til_dato"),
            rettPaaTiltakspenger = boolean("rett_paa_tiltakspenger"),
        )
    }

    private fun Row.toTiltakstypeAdminDto(): TiltakstypeAdminDto {
        val fraDato = localDate("fra_dato")
        val tilDato = localDate("til_dato")
        return TiltakstypeAdminDto(
            id = uuid("id"),
            navn = string("navn"),
            arenaKode = string("arena_kode"),
            registrertIArenaDato = localDateTime("registrert_dato_i_arena"),
            sistEndretIArenaDato = localDateTime("sist_endret_dato_i_arena"),
            fraDato = fraDato,
            tilDato = tilDato,
            sanityId = uuidOrNull("sanity_id"),
            rettPaaTiltakspenger = boolean("rett_paa_tiltakspenger"),
            status = Tiltakstypestatus.resolveFromDates(LocalDate.now(), fraDato, tilDato),
        )
    }

    private fun statuserWhereStatement(statuser: List<Tiltakstypestatus>): String =
        statuser
            .ifEmpty { null }
            ?.joinToString(prefix = "(", postfix = ")", separator = " or ") {
                when (it) {
                    Tiltakstypestatus.Planlagt -> "(:today < fra_dato)"
                    Tiltakstypestatus.Aktiv -> "(:today >= fra_dato and :today <= til_dato)"
                    else -> "(:today > til_dato)"
                }
            }
            ?: "true"
}
