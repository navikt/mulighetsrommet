package no.nav.mulighetsrommet.api.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.domain.Deltaker
import no.nav.mulighetsrommet.domain.Deltakerstatus
import org.slf4j.Logger

class DeltakerService(private val db: Database, private val logger: Logger) {

    fun getDeltakere(): List<Deltaker> {
        val query = """
            select id, tiltaksgjennomforing_id, person_id, fra_dato, til_dato, status from deltaker
        """.trimIndent()
        val queryResult = queryOf(query).map { toDeltaker(it) }.asList
        return db.session.run(queryResult)
    }

    fun createDeltaker(tiltaksgjennomforingId: Int, deltaker: Deltaker): Deltaker {
        val query = """
            insert into deltaker (tiltaksgjennomforing_id, person_id, fra_dato, til_dato, status) values (?, ?, ?, ?, ?::deltakerstatus) returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            tiltaksgjennomforingId,
            deltaker.personId,
            deltaker.fraDato,
            deltaker.tilDato,
            deltaker.status
        ).asExecute.query.map { toDeltaker(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun updateDeltaker(tiltaksgjennomforingId: Int, personId: Int, deltaker: Deltaker): Deltaker {
        val query = """
            update deltaker set tiltaksgjennomforing_id = ?, person_id = ?, fra_dato = ?, til_dato = ?, status = ?::deltakerstatus where tiltaksgjennomforing_id = ? and personId = ? returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            deltaker.tiltaksgjennomforingId,
            deltaker.personId,
            deltaker.fraDato,
            deltaker.tilDato,
            deltaker.status.name,
            tiltaksgjennomforingId,
            personId
        ).asExecute.query.map { toDeltaker(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    private fun toDeltaker(row: Row): Deltaker = Deltaker(
        id = row.int("id"),
        tiltaksgjennomforingId = row.int("tiltaksgjennomforing_id"),
        personId = row.int("person_id"),
        fraDato = row.localDateTimeOrNull("fra_dato"),
        tilDato = row.localDateTimeOrNull("til_dato"),
        status = Deltakerstatus.valueOf(row.string("status"))
    )
}
