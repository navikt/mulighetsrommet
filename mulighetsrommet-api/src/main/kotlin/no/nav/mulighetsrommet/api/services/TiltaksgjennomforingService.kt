package no.nav.mulighetsrommet.api.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakskode

class TiltaksgjennomforingService(private val db: Database) {

    fun getTiltaksgjennomforingerByTiltakskode(tiltakskode: Tiltakskode): List<Tiltaksgjennomforing> {
        val query = """
            select id, tittel, beskrivelse, tiltaksnummer, tiltakskode, fra_dato, til_dato from tiltaksgjennomforing where tiltakskode = ?
        """.trimIndent()
        val queryResult = queryOf(query, tiltakskode.name).map { toTiltaksgjennomforing(it) }.asList
        return db.session.run(queryResult)
    }

    fun getTiltaksgjennomforingById(id: Int): Tiltaksgjennomforing? {
        val query = """
            select id, tittel, beskrivelse, tiltaksnummer, tiltakskode, fra_dato, til_dato from tiltaksgjennomforing where id = ?
        """.trimIndent()
        val queryResult = queryOf(query, id).map { toTiltaksgjennomforing(it) }.asSingle
        return db.session.run(queryResult)
    }

    fun getTiltaksgjennomforinger(): List<Tiltaksgjennomforing> {
        val query = """
            select id, tittel, beskrivelse, tiltaksnummer, tiltakskode, fra_dato, til_dato from tiltaksgjennomforing
        """.trimIndent()
        val queryResult = queryOf(query).map { toTiltaksgjennomforing(it) }.asList
        return db.session.run(queryResult)
    }

    private fun toTiltaksgjennomforing(row: Row): Tiltaksgjennomforing =
        Tiltaksgjennomforing(
            id = row.int("id"),
            tittel = row.string("tittel"),
            beskrivelse = row.string("beskrivelse"),
            tiltaksnummer = row.int("tiltaksnummer"),
            tiltakskode = Tiltakskode.valueOf(row.string("tiltakskode")),
            fraDato = row.localDateTime("fra_dato"),
            tilDato = row.localDateTime("til_dato")
        )
}
