package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.services.Sokefilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltaksgjennomforing: TiltaksgjennomforingDbo): QueryResult<TiltaksgjennomforingDbo> = query {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltaksgjennomforing.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (id, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, fra_dato, til_dato, enhet)
            values (:id::uuid, :navn, :tiltakstype_id::uuid, :tiltaksnummer, :virksomhetsnummer, :fra_dato, :til_dato, :enhet)
            on conflict (id)
                do update set navn              = excluded.navn,
                              tiltakstype_id    = excluded.tiltakstype_id,
                              tiltaksnummer     = excluded.tiltaksnummer,
                              virksomhetsnummer = excluded.virksomhetsnummer,
                              fra_dato          = excluded.fra_dato,
                              til_dato          = excluded.til_dato,
                              enhet             = excluded.enhet
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
                   fra_dato,
                   til_dato,
                   tiltakskode,
                   t.navn as tiltakstype_navn,
                   enhet
            from tiltaksgjennomforing tg
                     join tiltakstype t on t.id = tg.tiltakstype_id
            where tg.id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toTiltaksgjennomforingDto() }
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
                   fra_dato,
                   til_dato,
                   t.navn           as tiltakstype_navn,
                   enhet,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                     join tiltakstype t on tg.tiltakstype_id = t.id
            limit ? offset ?
        """.trimIndent()
        val results = queryOf(query, pagination.limit, pagination.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingDto()
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
                   fra_dato,
                   til_dato,
                   t.navn           as tiltakstype_navn,
                   enhet,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                     join tiltakstype t on tg.tiltakstype_id = t.id
            where tg.tiltakstype_id = ?
            limit ? offset ?
        """.trimIndent()
        val results = queryOf(query, id, pagination.limit, pagination.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingDto()
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
                   fra_dato,
                   til_dato,
                   t.navn           as tiltakstype_navn,
                   enhet,
                   count(*) over () as full_count
            from tiltaksgjennomforing tg
                     join tiltakstype t on tg.tiltakstype_id = t.id
            where enhet = ?
            limit ? offset ?
        """.trimIndent()
        val results = queryOf(query, enhet, pagination.limit, pagination.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingDto()
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
                   fra_dato,
                   til_dato,
                   t.navn as tiltakstype_navn,
                   enhet
            from tiltaksgjennomforing tg
                     join tiltakstype t on tg.tiltakstype_id = t.id
            where tiltaksnummer like concat('%', ?, '%')
        """.trimIndent()
        return queryOf(query, filter.tiltaksnummer)
            .map {
                it.toTiltaksgjennomforingDto()
            }
            .asList
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter tiltaksgjennomføring id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    private fun TiltaksgjennomforingDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "tiltaksnummer" to tiltaksnummer,
        "virksomhetsnummer" to virksomhetsnummer,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "enhet" to enhet
    )

    private fun Row.toTiltaksgjennomforingDbo() = TiltaksgjennomforingDbo(
        id = uuid("id"),
        navn = stringOrNull("navn"),
        tiltakstypeId = uuid("tiltakstype_id"),
        tiltaksnummer = string("tiltaksnummer"),
        virksomhetsnummer = stringOrNull("virksomhetsnummer"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        enhet = string("enhet")
    )

    private fun Row.toTiltaksgjennomforingDto() = TiltaksgjennomforingAdminDto(
        id = uuid("id"),
        tiltakstype = TiltakstypeDto(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            arenaKode = string("tiltakskode")
        ),
        navn = stringOrNull("navn"),
        tiltaksnummer = string("tiltaksnummer"),
        virksomhetsnummer = stringOrNull("virksomhetsnummer"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        enhet = string("enhet")
    )
}
