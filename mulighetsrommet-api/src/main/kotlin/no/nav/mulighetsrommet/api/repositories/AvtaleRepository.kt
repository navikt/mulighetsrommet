package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class AvtaleRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(avtale: AvtaleDbo): QueryResult<AvtaleDbo> = query {
        logger.info("Lagrer avtale id=${avtale.id}")

        @Language("PostgreSQL")
        val query = """
            insert into avtale(id,
                               navn,
                               tiltakstype_id,
                               avtalenummer,
                               leverandor_organisasjonsnummer,
                               start_dato,
                               slutt_dato,
                               enhet,
                               avtaletype,
                               avtalestatus,
                               prisbetingelser)
            values (:id::uuid,
                    :navn,
                    :tiltakstype_id::uuid,
                    :avtalenummer,
                    :leverandor_organisasjonsnummer,
                    :start_dato,
                    :slutt_dato,
                    :enhet,
                    :avtaletype::avtaletype,
                    :avtalestatus::avtalestatus,
                    :prisbetingelser)
            on conflict (id) do update set navn                           = excluded.navn,
                                           tiltakstype_id                 = excluded.tiltakstype_id,
                                           avtalenummer                   = excluded.avtalenummer,
                                           leverandor_organisasjonsnummer = excluded.leverandor_organisasjonsnummer,
                                           start_dato                     = excluded.start_dato,
                                           slutt_dato                     = excluded.slutt_dato,
                                           enhet                          = excluded.enhet,
                                           avtaletype                     = excluded.avtaletype,
                                           avtalestatus                   = excluded.avtalestatus,
                                           prisbetingelser                = excluded.prisbetingelser
            returning *
        """.trimIndent()

        queryOf(query, avtale.toSqlParameters())
            .map { it.toAvtaleDbo() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun get(id: UUID): AvtaleAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select a.id,
                   a.navn,
                   a.tiltakstype_id,
                   a.avtalenummer,
                   a.leverandor_organisasjonsnummer,
                   a.start_dato,
                   a.slutt_dato,
                   a.enhet,
                   a.avtaletype,
                   a.avtalestatus,
                   a.prisbetingelser,
                   t.navn as tiltakstype_navn,
                   t.tiltakskode
            from avtale a
                     join tiltakstype t on t.id = a.tiltakstype_id
            where a.id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toAvtaleAdminDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(pagination: PaginationParams = PaginationParams()): Pair<Int, List<AvtaleAdminDto>> {
        @Language("PostgreSQL")
        val query = """
            select a.id,
                   a.navn,
                   a.tiltakstype_id,
                   a.avtalenummer,
                   a.leverandor_organisasjonsnummer,
                   a.start_dato,
                   a.slutt_dato,
                   a.enhet,
                   a.avtaletype,
                   a.avtalestatus,
                   a.prisbetingelser,
                   t.navn as tiltakstype_navn,
                   t.tiltakskode,
                   count(*) over () as full_count
            from avtale a
                     join tiltakstype t on a.tiltakstype_id = t.id
            order by a.navn
            limit ? offset ?
        """.trimIndent()
        val results = queryOf(query, pagination.limit, pagination.offset)
            .map {
                it.int("full_count") to it.toAvtaleAdminDto()
            }
            .asList
            .let { db.run(it) }

        val totaltAntall = results.firstOrNull()?.first ?: 0
        val avtaler = results.map { it.second }
        return Pair(totaltAntall, avtaler)
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter avtale id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from avtale
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    fun getAvtalerForTiltakstype(
        tiltakstypeId: UUID,
        pagination: PaginationParams = PaginationParams()
    ): Pair<Int, List<AvtaleAdminDto>> {
        logger.info("Henter avtaler for tiltakstype med id: '$tiltakstypeId'")
        @Language("PostgreSQL")
        val query = """
           select a.id,
                   a.navn,
                   a.tiltakstype_id,
                   a.avtalenummer,
                   a.leverandor_organisasjonsnummer,
                   a.start_dato,
                   a.slutt_dato,
                   a.enhet,
                   a.avtaletype,
                   a.avtalestatus,
                   a.prisbetingelser,
                   t.navn as tiltakstype_navn,
                   t.tiltakskode,
                   count(*) over () as full_count
            from avtale a
                     join tiltakstype t on a.tiltakstype_id = t.id
            where a.tiltakstype_id = ?
            order by a.navn
            limit ? offset ?
        """.trimIndent()

        val results = queryOf(query, tiltakstypeId, pagination.limit, pagination.offset)
            .map {
                it.int("full_count") to it.toAvtaleAdminDto()
            }
            .asList
            .let { db.run(it) }

        val totaltAntall = results.firstOrNull()?.first ?: 0
        val avtaler = results.map { it.second }
        return Pair(totaltAntall, avtaler)
    }

    private fun AvtaleDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "avtalenummer" to avtalenummer,
        "leverandor_organisasjonsnummer" to leverandorOrganisasjonsnummer,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "enhet" to enhet,
        "avtaletype" to avtaletype.name,
        "avtalestatus" to avtalestatus.name,
        "prisbetingelser" to prisbetingelser
    )

    private fun Row.toAvtaleDbo() = AvtaleDbo(
        id = uuid("id"),
        navn = string("navn"),
        tiltakstypeId = uuid("tiltakstype_id"),
        avtalenummer = string("avtalenummer"),
        leverandorOrganisasjonsnummer = string("leverandor_organisasjonsnummer"),
        startDato = localDate("start_dato"),
        sluttDato = localDate("slutt_dato"),
        enhet = string("enhet"),
        avtaletype = Avtaletype.valueOf(string("avtaletype")),
        avtalestatus = Avtalestatus.valueOf(string("avtalestatus")),
        prisbetingelser = stringOrNull("prisbetingelser")
    )

    private fun Row.toAvtaleAdminDto() = AvtaleAdminDto(
        id = uuid("id"),
        navn = string("navn"),
        tiltakstype = AvtaleAdminDto.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            arenaKode = string("tiltakskode")
        ),
        avtalenummer = string("avtalenummer"),
        leverandorOrganisasjonsnummer = string("leverandor_organisasjonsnummer"),
        startDato = localDate("start_dato"),
        sluttDato = localDate("slutt_dato"),
        enhet = string("enhet"),
        avtaletype = Avtaletype.valueOf(string("avtaletype")),
        avtalestatus = Avtalestatus.valueOf(string("avtalestatus")),
        prisbetingelser = stringOrNull("prisbetingelser")
    )
}
