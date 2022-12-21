package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingMedTiltakstypeEkstern
import no.nav.mulighetsrommet.api.routes.v1.TiltakstypeEkstern
import no.nav.mulighetsrommet.api.services.Sokefilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.TiltaksgjennomforingMedTiltakstype
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltaksgjennomforing: Tiltaksgjennomforing): QueryResult<Tiltaksgjennomforing> = query {
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
            .map { it.toTiltaksgjennomforing() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getByTiltakstypeId(id: UUID): List<Tiltaksgjennomforing> {
        @Language("PostgreSQL")
        val query = """
            select id::uuid, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, fra_dato, til_dato, enhet
            from tiltaksgjennomforing
            where tiltakstype_id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toTiltaksgjennomforing() }
            .asList
            .let { db.run(it) }
    }

    fun getAllByTiltakskode(
        tiltakskode: String,
        paginationParams: PaginationParams = PaginationParams()
    ): Pair<Int, List<TiltaksgjennomforingMedTiltakstype>> {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid, tg.navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, tiltakskode, fra_dato, til_dato, t.navn as tiltakstypeNavn, enhet, count(*) OVER() AS full_count
            from tiltaksgjennomforing tg
            join tiltakstype t on tg.tiltakstype_id = t.id
            where tiltakskode = ?
            limit ?
            offset ?
        """.trimIndent()
        val results = queryOf(query, tiltakskode, paginationParams.limit, paginationParams.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingMedTiltakstype()
            }
            .asList
            .let { db.run(it) }
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
    }

    fun get(id: UUID): Tiltaksgjennomforing? {
        @Language("PostgreSQL")
        val query = """
            select id::uuid, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, fra_dato, til_dato, enhet
            from tiltaksgjennomforing
            where id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toTiltaksgjennomforing() }
            .asSingle
            .let { db.run(it) }
    }

    fun getForExternalWithTiltakstypedata(id: UUID): TiltaksgjennomforingMedTiltakstypeEkstern? {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid, tg.navn, fra_dato, til_dato, tiltakskode, t.navn as tiltakstypeNavn, t.id as tiltakstypeId, t.tiltakskode as tiltakstypekode
            from tiltaksgjennomforing tg
            join tiltakstype t on t.id = tg.tiltakstype_id
            where tg.id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toTiltaksgjennomforingMedTiltakstypeEkstern() }
            .asSingle
            .let { db.run(it) }
    }

    fun getWithTiltakstypedata(id: UUID): TiltaksgjennomforingMedTiltakstype? {
        @Language("PostgreSQL")
        val query = """
            select tg.id::uuid, tg.navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, fra_dato, til_dato, tiltakskode, t.navn as tiltakstypeNavn, enhet
            from tiltaksgjennomforing tg
            join tiltakstype t on t.id = tg.tiltakstype_id
            where tg.id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toTiltaksgjennomforingMedTiltakstype() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(paginationParams: PaginationParams = PaginationParams()): Pair<Int, List<Tiltaksgjennomforing>> {
        @Language("PostgreSQL")
        val query = """
            select tg.id, tg.navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, tiltakskode, fra_dato, til_dato, enhet, count(*) OVER() AS full_count
            from tiltaksgjennomforing tg
            join tiltakstype t on tg.tiltakstype_id = t.id
            limit ?
            offset ?
        """.trimIndent()
        val results = queryOf(query, paginationParams.limit, paginationParams.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforing()
            }
            .asList
            .let { db.run(it) }
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
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

    fun sok(filter: Sokefilter): List<TiltaksgjennomforingMedTiltakstype> {
        val query = """
            select tg.id::uuid, tg.navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, tiltakskode, fra_dato, til_dato, t.navn as tiltakstypeNavn, enhet
            from tiltaksgjennomforing tg
            join tiltakstype t on tg.tiltakstype_id = t.id
            where tiltaksnummer like concat('%', ?, '%')
        """.trimIndent()
        return queryOf(query, filter.tiltaksnummer)
            .map {
                it.toTiltaksgjennomforingMedTiltakstype()
            }
            .asList
            .let { db.run(it) }
    }

    fun getAllByEnhet(enhet: String, paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingMedTiltakstype>> {
        val query = """
            select tg.id::uuid, tg.navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, tiltakskode, fra_dato, til_dato, t.navn as tiltakstypeNavn, enhet, count(*) OVER() AS full_count
            from tiltaksgjennomforing tg
            join tiltakstype t on tg.tiltakstype_id = t.id
            where enhet = ?
            limit ?
            offset ?
        """.trimIndent()
        val results = queryOf(query, enhet, paginationParams.limit, paginationParams.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingMedTiltakstype()
            }
            .asList
            .let { db.run(it) }
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
    }

    fun getAllWithTiltakstypedata(paginationParams: PaginationParams): Pair<Int, List<TiltaksgjennomforingMedTiltakstype>> {
        @Language("PostgreSQL")
        val query = """
           select tg.id::uuid, tg.navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, tiltakskode, fra_dato, til_dato, t.navn as tiltakstypeNavn, enhet, count(*) OVER() AS full_count
            from tiltaksgjennomforing tg
            join tiltakstype t on tg.tiltakstype_id = t.id
            limit ?
            offset ?
        """.trimIndent()
        val results = queryOf(query, paginationParams.limit, paginationParams.offset)
            .map {
                it.int("full_count") to it.toTiltaksgjennomforingMedTiltakstype()
            }
            .asList
            .let { db.run(it) }
        val tiltaksgjennomforinger = results.map { it.second }
        val totaltAntall = results.firstOrNull()?.first ?: 0

        return Pair(totaltAntall, tiltaksgjennomforinger)
    }

    private fun Tiltaksgjennomforing.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "tiltaksnummer" to tiltaksnummer,
        "virksomhetsnummer" to virksomhetsnummer,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "enhet" to enhet
    )

    private fun Row.toTiltaksgjennomforing() = Tiltaksgjennomforing(
        id = uuid("id"),
        navn = stringOrNull("navn"),
        tiltakstypeId = uuid("tiltakstype_id"),
        tiltaksnummer = string("tiltaksnummer"),
        virksomhetsnummer = stringOrNull("virksomhetsnummer"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        enhet = stringOrNull("enhet")
    )

    private fun Row.toTiltaksgjennomforingMedTiltakstype() = TiltaksgjennomforingMedTiltakstype(
        id = uuid("id"),
        navn = stringOrNull("navn"),
        tiltakstypeId = uuid("tiltakstype_id"),
        tiltaksnummer = string("tiltaksnummer"),
        virksomhetsnummer = stringOrNull("virksomhetsnummer"),
        tiltakskode = string("tiltakskode"),
        tiltakstypeNavn = string("tiltakstypeNavn"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        enhet = stringOrNull("enhet")
    )

    private fun Row.toTiltaksgjennomforingMedTiltakstypeEkstern() = TiltaksgjennomforingMedTiltakstypeEkstern(
        id = uuid("id"),
        navn = stringOrNull("navn") ?: "", // Hva skal skje her?
        tiltakstype = TiltakstypeEkstern(
            id = uuid("tiltakstypeId"),
            navn = string("tiltakstypeNavn"),
            arenaKode = string("tiltakstypekode"),
        ),
        startDato = localDateTimeOrNull("fra_dato"),
        sluttDato = localDateTimeOrNull("til_dato"),
    )
}
