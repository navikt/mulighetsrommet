package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.Deltaker
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class DeltakerRepository(private val db: Database) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun upsert(deltaker: Deltaker) = query {
        logger.info("Lagrer deltaker id=${deltaker.id}, tiltak=${deltaker.tiltaksgjennomforingId}")

        @Language("PostgreSQL")
        val query = """
            insert into deltaker (id, tiltaksdeltaker_id, tiltaksgjennomforing_id, person_id, status, fra_dato, til_dato, registrert_dato)
            values (:id::uuid, :tiltaksdeltaker_id, :tiltaksgjennomforing_id, :person_id, :status, :fra_dato, :til_dato, :registrert_dato)
            on conflict (id)
                do update set tiltaksdeltaker_id      = excluded.tiltaksdeltaker_id,
                              tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                              person_id               = excluded.person_id,
                              status                  = excluded.status,
                              fra_dato                = excluded.fra_dato,
                              til_dato                = excluded.til_dato,
                              registrert_dato         = excluded.registrert_dato
            returning *
        """.trimIndent()

        queryOf(query, deltaker.toSqlParameters())
            .map { it.toDeltaker() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            delete from deltaker
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    fun get(id: UUID): Deltaker? {
        logger.info("Henter deltaker id=$id")

        @Language("PostgreSQL")
        val query = """
            select id, tiltaksdeltaker_id, tiltaksgjennomforing_id, person_id, status, fra_dato, til_dato, registrert_dato
            from deltaker
            where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toDeltaker() }
            .asSingle
            .let { db.run(it) }
    }

    fun getByTiltaksgjennomforingId(id: Int): List<Deltaker> {
        @Language("PostgreSQL")
        val query = """
            select id, tiltaksdeltaker_id, tiltaksgjennomforing_id, person_id, status, fra_dato, til_dato, registrert_dato
            from deltaker
            where tiltaksgjennomforing_id = ?
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toDeltaker() }
            .asList
            .let { db.run(it) }
    }

    private fun Deltaker.toSqlParameters() = mapOf(
        "id" to id,
        "tiltaksdeltaker_id" to tiltaksdeltakerId,
        "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
        "person_id" to personId,
        "status" to status.name,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "registrert_dato" to registrertDato,
    )

    private fun Row.toDeltaker() = Deltaker(
        id = uuid("id"),
        tiltaksdeltakerId = int("tiltaksdeltaker_id"),
        tiltaksgjennomforingId = int("tiltaksgjennomforing_id"),
        personId = int("person_id"),
        status = Deltakerstatus.valueOf(string("status")),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        registrertDato = localDateTime("registrert_dato"),
    )
}
