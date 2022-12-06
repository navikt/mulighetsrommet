package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltakstypeRepository(private val db: Database) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltak: Tiltakstype) = query {
        logger.info("Lagrer tiltakstype id=${tiltak.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (id, navn, tiltakskode, fra_dato, til_dato)
            values (:id::uuid, :navn, :tiltakskode, :fra_dato, :til_dato)
            on conflict (id)
                do update set navn          = excluded.navn,
                              tiltakskode   = excluded.tiltakskode,
                              fra_dato      = excluded.fra_dato,
                              til_dato      = excluded.til_dato
            returning *
        """.trimIndent()

        queryOf(query, tiltak.toSqlParameters())
            .map { it.toSak() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(tiltak: Tiltakstype): QueryResult<Tiltakstype> = query {
        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, tiltak.id)
            .asExecute
            .let { db.run(it) }

        tiltak
    }

    fun get(id: UUID): Tiltakstype? {
        logger.info("Henter tiltakstype id=$id")

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, fra_dato, til_dato
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toSak() }
            .asSingle
            .let { db.run(it) }
    }

    private fun Tiltakstype.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakskode" to tiltakskode,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
    )

    private fun Row.toSak() = Tiltakstype(
        id = uuid("id"),
        navn = string("navn"),
        tiltakskode = string("tiltakskode"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato")
    )
}
