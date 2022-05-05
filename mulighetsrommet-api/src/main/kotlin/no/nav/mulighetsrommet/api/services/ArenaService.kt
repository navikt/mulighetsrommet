package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.domain.Deltaker
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype
import org.slf4j.Logger

class ArenaService(private val db: Database, private val logger: Logger) {

    fun createTiltakstype(tiltakstype: Tiltakstype): Tiltakstype {
        val query = """
            insert into tiltakstype (navn, innsatsgruppe_id, sanity_id, tiltakskode, fra_dato, til_dato) values (?, ?, ?, ?::tiltakskode, ?, ?) returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            tiltakstype.navn,
            tiltakstype.innsatsgruppe,
            tiltakstype.sanityId,
            tiltakstype.tiltakskode.name,
            tiltakstype.fraDato,
            tiltakstype.tilDato
        ).asExecute.query.map { DatabaseMapper.toTiltakstype(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun updateTiltakstype(tiltakskode: Tiltakskode, tiltakstype: Tiltakstype): Tiltakstype {
        val query = """
            update tiltakstype set navn = ?, innsatsgruppe_id = ?, sanity_id = ?, fra_dato = ?, til_dato = ? where tiltakskode::text = ? returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            tiltakstype.navn,
            tiltakstype.innsatsgruppe,
            tiltakstype.sanityId,
            tiltakstype.fraDato,
            tiltakstype.tilDato,
            tiltakskode.name
        ).asExecute.query.map { DatabaseMapper.toTiltakstype(it) }.asSingle
        return db.session.run(queryResult)!!
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
        ).asExecute.query.map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
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
        ).asExecute.query.map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun createDeltaker(deltaker: Deltaker): Deltaker {
        val query = """
            insert into deltaker (arena_id, tiltaksgjennomforing_id, person_id, fra_dato, til_dato, status) values (?, ?, ?, ?, ?, ?::deltakerstatus) returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            deltaker.arenaId,
            deltaker.tiltaksgjennomforingId,
            deltaker.personId,
            deltaker.fraDato,
            deltaker.tilDato,
            deltaker.status.name
        ).asExecute.query.map { DatabaseMapper.toDeltaker(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun updateDeltaker(arenaId: Int, deltaker: Deltaker): Deltaker {
        val query = """
            update deltaker set tiltaksgjennomforing_id = ?, person_id = ?, fra_dato = ?, til_dato = ?, status = ?::deltakerstatus where arena_id = ? returning *
        """.trimIndent()
        val queryResult = queryOf(
            query,
            deltaker.tiltaksgjennomforingId,
            deltaker.personId,
            deltaker.fraDato,
            deltaker.tilDato,
            deltaker.status.name,
            arenaId,
        ).asExecute.query.map { DatabaseMapper.toDeltaker(it) }.asSingle
        return db.session.run(queryResult)!!
    }
}
