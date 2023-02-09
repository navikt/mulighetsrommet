package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.Deltaker
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
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
            insert into deltaker (id, tiltaksdeltaker_id, tiltaksgjennomforing_id, person_id, fra_dato, til_dato, status)
            values (:id::uuid, :tiltaksdeltaker_id, :tiltaksgjennomforing_id, :person_id, :fra_dato, :til_dato, :status)
            on conflict (id)
                do update set tiltaksdeltaker_id      = excluded.tiltaksdeltaker_id,
                              tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                              person_id               = excluded.person_id,
                              fra_dato                = excluded.fra_dato,
                              til_dato                = excluded.til_dato,
                              status                  = excluded.status
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
            select id, tiltaksdeltaker_id, tiltaksgjennomforing_id, person_id, fra_dato, til_dato, status
            from deltaker
            where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toDeltaker() }
            .asSingle
            .let { db.run(it) }
    }

    private fun Deltaker.toSqlParameters() = mapOf(
        "id" to id,
        "tiltaksdeltaker_id" to tiltaksdeltakerId,
        "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
        "person_id" to personId,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "status" to status.name,
    )

    private fun Row.toDeltaker() = Deltaker(
        id = uuid("id"),
        tiltaksdeltakerId = int("tiltaksdeltaker_id"),
        tiltaksgjennomforingId = int("tiltaksgjennomforing_id"),
        personId = int("person_id"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        status = Deltakerstatus.valueOf(string("status"))
    )
}
