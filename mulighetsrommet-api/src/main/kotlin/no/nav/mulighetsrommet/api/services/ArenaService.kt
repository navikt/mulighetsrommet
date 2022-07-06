package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.domain.Deltaker
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakstype
import no.nav.mulighetsrommet.domain.adapter.ArenaSak
import org.intellij.lang.annotations.Language
import org.slf4j.Logger

class ArenaService(private val db: Database, private val logger: Logger) {

    fun upsertTiltakstype(tiltakstype: Tiltakstype): Tiltakstype {
        logger.info("Lagrer tiltakstype tiltakskode={} ", tiltakstype.tiltakskode)

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (navn, innsatsgruppe_id, sanity_id, tiltakskode, fra_dato, til_dato)
            values (?, ?, ?, ?, ?, ?)
            on conflict (tiltakskode)
            do update set
                navn = excluded.navn,
                innsatsgruppe_id = excluded.innsatsgruppe_id,
                sanity_id = excluded.sanity_id,
                tiltakskode = excluded.tiltakskode,
                fra_dato = excluded.fra_dato,
                til_dato = excluded.til_dato
            returning *
        """.trimIndent()

        val queryResult = queryOf(
            query,
            tiltakstype.navn,
            tiltakstype.innsatsgruppe,
            tiltakstype.sanityId,
            tiltakstype.tiltakskode,
            tiltakstype.fraDato,
            tiltakstype.tilDato
        ).map { DatabaseMapper.toTiltakstype(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun upsertTiltaksgjennomforing(tiltaksgjennomforing: Tiltaksgjennomforing): Tiltaksgjennomforing {
        logger.info(
            "Lagrer tiltak tiltakskode={} sakId={}",
            tiltaksgjennomforing.tiltakskode,
            tiltaksgjennomforing.sakId
        )

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (navn, arrangor_id, tiltakskode, tiltaksnummer, arena_id, sanity_id, fra_dato, til_dato, sak_id)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (arena_id)
            do update set
                navn = excluded.navn,
                arrangor_id = excluded.arrangor_id,
                tiltakskode = excluded.tiltakskode,
                tiltaksnummer = excluded.tiltaksnummer,
                arena_id = excluded.arena_id,
                sanity_id = excluded.sanity_id,
                fra_dato = excluded.fra_dato,
                til_dato = excluded.til_dato,
                sak_id = excluded.sak_id
            returning *
        """.trimIndent()

        val queryResult = queryOf(
            query,
            tiltaksgjennomforing.navn,
            tiltaksgjennomforing.arrangorId,
            tiltaksgjennomforing.tiltakskode,
            tiltaksgjennomforing.tiltaksnummer,
            tiltaksgjennomforing.arenaId,
            tiltaksgjennomforing.sanityId,
            tiltaksgjennomforing.fraDato,
            tiltaksgjennomforing.tilDato,
            tiltaksgjennomforing.sakId
        ).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun upsertDeltaker(deltaker: Deltaker): Deltaker {
        logger.info("Lagrer deltaker tiltak={}", deltaker.tiltaksgjennomforingId)

        @Language("PostgreSQL")
        val query = """
            insert into deltaker (arena_id, tiltaksgjennomforing_id, person_id, fra_dato, til_dato, status)
            values (?, ?, ?, ?, ?, ?::deltakerstatus)
            on conflict (arena_id)
            do update set
                arena_id = excluded.arena_id,
                tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                person_id = excluded.person_id,
                fra_dato = excluded.fra_dato,
                til_dato = excluded.til_dato,
                status = excluded.status
            returning *
        """.trimIndent()

        val queryResult = queryOf(
            query,
            deltaker.arenaId,
            deltaker.tiltaksgjennomforingId,
            deltaker.personId,
            deltaker.fraDato,
            deltaker.tilDato,
            deltaker.status.name
        ).map { DatabaseMapper.toDeltaker(it) }.asSingle
        return db.session.run(queryResult)!!
    }

    fun updateTiltaksgjennomforingWithSak(sak: ArenaSak): Tiltaksgjennomforing? {
        logger.info("Oppdaterer tiltak med sak sakId={} tiltaksnummer={}", sak.sakId, sak.lopenrsak)

        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing set tiltaksnummer = ?, aar = ? where sak_id = ? returning *
        """.trimIndent()

        val queryResult = queryOf(
            query,
            sak.lopenrsak,
            sak.aar,
            sak.sakId,
        ).map { DatabaseMapper.toTiltaksgjennomforing(it) }.asSingle
        return db.session.run(queryResult)
    }
}
