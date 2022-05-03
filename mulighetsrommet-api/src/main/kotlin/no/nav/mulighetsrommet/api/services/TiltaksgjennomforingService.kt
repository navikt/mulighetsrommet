package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakskode
import org.slf4j.Logger

class TiltaksgjennomforingService(private val db: Database, private val logger: Logger) {

    fun getTiltaksgjennomforingerByTiltakskode(tiltakskode: Tiltakskode): List<Tiltaksgjennomforing> {
        val query = """
            select id, navn, tiltaksnummer, arrangor_id, tiltakskode, arena_id, sak_id, sanity_id, fra_dato, til_dato
            from tiltaksgjennomforing
            where tiltakskode::text = ?
        """.trimIndent()
        val queryResult = queryOf(query, tiltakskode.name).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asList
        return db.session.run(queryResult)
    }

    fun getTiltaksgjennomforingById(id: Int): Tiltaksgjennomforing? {
        val query = """
            select id, navn, tiltaksnummer, arrangor_id, tiltakskode, arena_id, sak_id, sanity_id, fra_dato, til_dato
            from tiltaksgjennomforing
            where id = ?
        """.trimIndent()
        val queryResult = queryOf(query, id).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
        return db.session.run(queryResult)
    }

    fun getTiltaksgjennomforinger(): List<Tiltaksgjennomforing> {
        val query = """
            select id, navn, tiltaksnummer, arrangor_id, tiltakskode, arena_id, sak_id, sanity_id, fra_dato, til_dato
            from tiltaksgjennomforing
        """.trimIndent()
        val queryResult = queryOf(query).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asList
        return db.session.run(queryResult)
    }

    fun createTiltaksgjennomforing(tiltaksgjennomforing: Tiltaksgjennomforing): Tiltaksgjennomforing {
        val query = """
            insert into tiltaksgjennomforing (navn, arrangor_id, tiltakskode, tiltaksnummer, arena_id, sak_id, sanity_id, fra_dato, til_dato) values (?, ?, ?::tiltakskode, ?, ?, ?, ?, ?, ?) returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            tiltaksgjennomforing.navn,
            tiltaksgjennomforing.arrangorId,
            tiltaksgjennomforing.tiltakskode.name,
            tiltaksgjennomforing.tiltaksnummer,
            tiltaksgjennomforing.arenaId,
            tiltaksgjennomforing.sakId,
            tiltaksgjennomforing.sanityId,
            tiltaksgjennomforing.fraDato,
            tiltaksgjennomforing.tilDato,
        ).asExecute.query.map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun updateTiltaksgjennomforing(arenaId: Int, tiltaksgjennomforing: Tiltaksgjennomforing): Tiltaksgjennomforing {
        val query = """
            update tiltaksgjennomforing set navn = ?, arrangor_id = ?, tiltakskode = ?::tiltakskode, tiltaksnummer = ?, sak_id = ?, sanity_id = ?, fra_dato = ?, til_dato = ? where arena_id = ? returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            tiltaksgjennomforing.navn,
            tiltaksgjennomforing.arrangorId,
            tiltaksgjennomforing.tiltakskode.name,
            tiltaksgjennomforing.tiltaksnummer,
            tiltaksgjennomforing.sakId,
            tiltaksgjennomforing.sanityId,
            tiltaksgjennomforing.fraDato,
            tiltaksgjennomforing.tilDato,
            arenaId
        ).asExecute.query.map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
        return db.session.run(queryResult)!!
    }
}
