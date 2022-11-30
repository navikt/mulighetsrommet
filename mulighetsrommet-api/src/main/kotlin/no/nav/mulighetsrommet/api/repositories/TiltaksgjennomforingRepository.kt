package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltaksgjennomforing: Tiltaksgjennomforing): QueryResult<Tiltaksgjennomforing> = query {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltaksgjennomforing.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (id, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, tilgjengelighet, antall_plasser)
            values (:id::uuid, :navn, :tiltakstype_id::uuid, :tiltaksnummer, :virksomhetsnummer, :tilgjengelighet::tilgjengelighetsstatus, :antall_plasser)
            on conflict (id)
                do update set navn              = excluded.navn,
                              tiltakstype_id    = excluded.tiltakstype_id,
                              tiltaksnummer     = excluded.tiltaksnummer,
                              virksomhetsnummer = excluded.virksomhetsnummer,
                              tilgjengelighet   = excluded.tilgjengelighet,
                              antall_plasser    = excluded.antall_plasser
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
            select id::uuid, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, tilgjengelighet, antall_plasser
            from tiltaksgjennomforing
            where tiltakstype_id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toTiltaksgjennomforing() }
            .asList
            .let { db.run(it) }
    }

    fun get(id: UUID): Tiltaksgjennomforing? {
        @Language("PostgreSQL")
        val query = """
            select id::uuid, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, tilgjengelighet, antall_plasser
            from tiltaksgjennomforing
            where id = ?::uuid
        """.trimIndent()
        return queryOf(query, id)
            .map { it.toTiltaksgjennomforing() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(paginationParams: PaginationParams = PaginationParams()): Pair<Int, List<Tiltaksgjennomforing>> {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakstype_id, tiltaksnummer, virksomhetsnummer, tilgjengelighet, antall_plasser, count(*) over() as full_count
            from tiltaksgjennomforing
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

    private fun Tiltaksgjennomforing.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "tiltaksnummer" to tiltaksnummer,
        "virksomhetsnummer" to virksomhetsnummer,
        "tilgjengelighet" to tilgjengelighet.name,
        "antall_plasser" to antallPlasser
    )

    private fun Row.toTiltaksgjennomforing() = Tiltaksgjennomforing(
        id = uuid("id"),
        navn = string("navn"),
        tiltakstypeId = uuid("tiltakstype_id"),
        tiltaksnummer = string("tiltaksnummer"),
        virksomhetsnummer = string("virksomhetsnummer"),
        tilgjengelighet = Tiltaksgjennomforing.Tilgjengelighetsstatus.valueOf(string("tilgjengelighet")),
        antallPlasser = intOrNull("antall_plasser")
    )
}
