package no.nav.mulighetsrommet.api.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakskode
import org.slf4j.Logger

class TiltaksgjennomforingService(private val db: Database, private val logger: Logger) {

    fun getTiltaksgjennomforingerByTiltakskode(tiltakskode: Tiltakskode): List<Tiltaksgjennomforing> {
        val query = """
            select id, tittel, beskrivelse, tiltaksnummer, tiltakskode, fra_dato, til_dato from tiltaksgjennomforing where tiltakskode::text = ?
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

    fun createTiltaksgjennomforing(tiltaksgjennomforing: Tiltaksgjennomforing): Tiltaksgjennomforing {
        val query = """
            insert into tiltaksgjennomforing (navn, arrangor_id, tiltakskode, tiltaksnummer, arena_id, sanity_id, fra_dato, til_dato, sak_id) values (?, ?, ?::tiltakskode, ?, ?, ?, ?, ?, ?) returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            tiltaksgjennomforing.navn,
            tiltaksgjennomforing.arrangorId,
            tiltaksgjennomforing.tiltakskode.name,
            tiltaksgjennomforing.tiltaksnummer,
            tiltaksgjennomforing.arenaId,
            tiltaksgjennomforing.sanityId,
            tiltaksgjennomforing.fraDato,
            tiltaksgjennomforing.tilDato,
            tiltaksgjennomforing.sakId
        ).asExecute.query.map { toTiltaksgjennomforing(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun updateTiltaksgjennomforing(arenaId: Int, tiltaksgjennomforing: Tiltaksgjennomforing): Tiltaksgjennomforing {
        val query = """
            update tiltaksgjennomforing set navn = ?, arrangor_id = ?, tiltakskode = ?::tiltakskode, sanity_id = ?, fra_dato = ?, til_dato = ? where arena_id = ? returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            tiltaksgjennomforing.navn,
            tiltaksgjennomforing.arrangorId,
            tiltaksgjennomforing.tiltakskode.name,
            tiltaksgjennomforing.sanityId,
            tiltaksgjennomforing.fraDato,
            tiltaksgjennomforing.tilDato,
            arenaId
        ).asExecute.query.map { toTiltaksgjennomforing(it) }.asSingle
        val lol = db.session.run(queryResult)!!
        return lol
//        return db.session.run(queryResult)!!
    }

    private fun toTiltaksgjennomforing(row: Row): Tiltaksgjennomforing =
        Tiltaksgjennomforing(
            id = row.int("id"),
            navn = row.string("navn"),
            tiltaksnummer = row.int("tiltaksnummer"),
            arrangorId = row.intOrNull("arrangor_id"),
            tiltakskode = Tiltakskode.valueOf(row.string("tiltakskode")),
            arenaId = row.int("arena_id"),
            sanityId = row.intOrNull("sanity_id"),
            fraDato = row.localDateTimeOrNull("fra_dato"),
            tilDato = row.localDateTimeOrNull("til_dato"),
            sakId = row.int("sak_id")
        )
}
