package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.services.Sokefilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class TiltaksgjennomforingRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingDbo> = query {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltaksgjennomforing.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (id, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, start_dato, slutt_dato, enhet, avslutningsstatus)
            values (:id::uuid, :navn, :tiltakstype_id::uuid, :tiltaksnummer, :virksomhetsnummer, :start_dato, :slutt_dato, :enhet, :avslutningsstatus::avslutningsstatus)
            on conflict (id)
                do update set navn              = excluded.navn,
                              tiltakstype_id    = excluded.tiltakstype_id,
                              tiltaksnummer     = excluded.tiltaksnummer,
                              virksomhetsnummer = excluded.virksomhetsnummer,
                              start_dato        = excluded.start_dato,
                              slutt_dato        = excluded.slutt_dato,
                              enhet             = excluded.enhet,
                              avslutningsstatus = excluded.avslutningsstatus
            returning *
        """.trimIndent()

        queryOf(query, tiltaksgjennomforing.toSqlParameters())
            .map { it.toTiltaksgjennomforingDbo() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun get(id: UUID): TiltaksgjennomforingAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tiltakstype_id,
                   tiltaksnummer,
                   virksomhetsnummer,
                   start_dato,
                   slutt_dato,
                   tiltakskode,
                   t.navn as tiltakstype_navn,
                   enhet,
                   avslutningsstatus
            from tiltaksgjennomforing tg
                     join tiltakstype t on t.id = tg.tiltakstype_id
            where tg.id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toTiltaksgjennomforingAdminDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(pagination: PaginationParams = PaginationParams()): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tiltakstype_id,
                   tiltaksnummer,
                   virksomhetsnummer,
                   tiltakskode,
                   start_dato,
                   slutt_dato,
                   t.navn           as tiltakstype_navn,
                   enhet,
                   avslutningsstatus,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                     join tiltakstype t on tg.tiltakstype_id = t.id
            order by tg.navn asc
            limit ? offset ?
        """.trimIndent()
        val results = queryOf(query, pagination.limit, pagination.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingAdminDto()
            }
            .asList
            .let { db.run(it) }
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
    }

    fun getAllByTiltakstypeId(
        id: UUID,
        pagination: PaginationParams = PaginationParams()
    ): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tiltakstype_id,
                   tiltaksnummer,
                   virksomhetsnummer,
                   tiltakskode,
                   start_dato,
                   slutt_dato,
                   t.navn           as tiltakstype_navn,
                   enhet,
                   avslutningsstatus,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                     join tiltakstype t on tg.tiltakstype_id = t.id
            where tg.tiltakstype_id = ?
            order by tg.navn asc
            limit ? offset ?
        """.trimIndent()
        val results = queryOf(query, id, pagination.limit, pagination.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingAdminDto()
            }
            .asList
            .let { db.run(it) }
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
    }

    fun getAllByEnhet(
        enhet: String,
        pagination: PaginationParams
    ): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tiltakstype_id,
                   tiltaksnummer,
                   virksomhetsnummer,
                   tiltakskode,
                   start_dato,
                   slutt_dato,
                   t.navn           as tiltakstype_navn,
                   enhet,
                   avslutningsstatus,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                     join tiltakstype t on tg.tiltakstype_id = t.id
            where enhet = ?
            order by tg.navn asc
            limit ? offset ?
        """.trimIndent()
        val results = queryOf(query, enhet, pagination.limit, pagination.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingAdminDto()
            }
            .asList
            .let { db.run(it) }
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
    }

    fun getAllByNavident(
        navIdent: String,
        pagination: PaginationParams
    ): Pair<Int, List<TiltaksgjennomforingAdminDto>> {
        logger.info("Henter alle tiltaksgjennomføringer for ansatt")

        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tiltakstype_id,
                   tiltaksnummer,
                   virksomhetsnummer,
                   tiltakskode,
                   start_dato,
                   slutt_dato,
                   t.navn           as tiltakstype_navn,
                   enhet,
                   avslutningsstatus,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                     join tiltakstype t on tg.tiltakstype_id = t.id
                     join ansatt_tiltaksgjennomforing a on tg.id = a.tiltaksgjennomforing_id
            where a.navident = ?
            order by tg.navn asc
            limit ? offset ?
        """.trimIndent()
        val results = queryOf(query, navIdent, pagination.limit, pagination.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingAdminDto()
            }
            .asList
            .let { db.run(it) }
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
    }

    fun sok(filter: Sokefilter): List<TiltaksgjennomforingAdminDto> {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid,
                   tg.navn,
                   tiltakstype_id,
                   tiltaksnummer,
                   virksomhetsnummer,
                   tiltakskode,
                   start_dato,
                   slutt_dato,
                   t.navn as tiltakstype_navn,
                   enhet,
                   avslutningsstatus
            from tiltaksgjennomforing tg
                     join tiltakstype t on tg.tiltakstype_id = t.id
            where tiltaksnummer like concat('%', ?, '%')
            order by tg.navn asc
        """.trimIndent()
        return queryOf(query, filter.tiltaksnummer)
            .map {
                it.toTiltaksgjennomforingAdminDto()
            }
            .asList
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter tiltaksgjennomføring id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    private fun TiltaksgjennomforingDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "tiltaksnummer" to tiltaksnummer,
        "virksomhetsnummer" to virksomhetsnummer,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "enhet" to enhet,
        "avslutningsstatus" to avslutningsstatus.name
    )

    private fun Row.toTiltaksgjennomforingDbo() = TiltaksgjennomforingDbo(
        id = uuid("id"),
        navn = stringOrNull("navn"),
        tiltakstypeId = uuid("tiltakstype_id"),
        tiltaksnummer = string("tiltaksnummer"),
        virksomhetsnummer = stringOrNull("virksomhetsnummer"),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        enhet = string("enhet"),
        avslutningsstatus = Avslutningsstatus.valueOf(string("avslutningsstatus"))
    )

    private fun Row.toTiltaksgjennomforingAdminDto(): TiltaksgjennomforingAdminDto {
        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")
        return TiltaksgjennomforingAdminDto(
            id = uuid("id"),
            tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                id = uuid("tiltakstype_id"),
                navn = string("tiltakstype_navn"),
                arenaKode = string("tiltakskode")
            ),
            navn = stringOrNull("navn"),
            tiltaksnummer = string("tiltaksnummer"),
            virksomhetsnummer = stringOrNull("virksomhetsnummer"),
            startDato = startDato,
            sluttDato = sluttDato,
            enhet = string("enhet"),
            status = Tiltaksgjennomforingsstatus.fromDbo(
                LocalDate.now(),
                startDato,
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus"))
            )
        )
    }
}
