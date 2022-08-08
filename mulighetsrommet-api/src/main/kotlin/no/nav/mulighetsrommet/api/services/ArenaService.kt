package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class ArenaService(private val db: Database) {
    private val logger = LoggerFactory.getLogger(ArenaService::class.java)

    fun upsertTiltakstype(tiltakstype: AdapterTiltak): AdapterTiltak {
        logger.info("Lagrer tiltakstype tiltakskode={} ", tiltakstype.tiltakskode)

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (navn, innsatsgruppe_id, tiltakskode, fra_dato, til_dato)
            values (?, ?, ?, ?, ?)
            on conflict (tiltakskode)
            do update set
                navn = excluded.navn,
                innsatsgruppe_id = excluded.innsatsgruppe_id,
                tiltakskode = excluded.tiltakskode,
                fra_dato = excluded.fra_dato,
                til_dato = excluded.til_dato
            returning *
        """.trimIndent()

        val queryResult = queryOf(
            query,
            tiltakstype.navn,
            tiltakstype.innsatsgruppe,
            tiltakstype.tiltakskode,
            tiltakstype.fraDato,
            tiltakstype.tilDato
        ).map { DatabaseMapper.toAdapterTiltak(it) }.asSingle
        return db.run(queryResult)!!
    }

    fun upsertTiltaksgjennomforing(tiltak: AdapterTiltaksgjennomforing): AdapterTiltaksgjennomforing {
        logger.info("Lagrer tiltak tiltakskode=${tiltak.tiltakskode} sakId=${tiltak.sakId}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (navn,
                                              arrangor_id,
                                              tiltakskode,
                                              arena_id,
                                              fra_dato,
                                              til_dato, sak_id,
                                              apent_for_innsok,
                                              antall_plasser)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (arena_id)
                do update set navn             = excluded.navn,
                              arrangor_id      = excluded.arrangor_id,
                              tiltakskode      = excluded.tiltakskode,
                              arena_id         = excluded.arena_id,
                              fra_dato         = excluded.fra_dato,
                              til_dato         = excluded.til_dato,
                              sak_id           = excluded.sak_id,
                              apent_for_innsok = excluded.apent_for_innsok,
                              antall_plasser   = excluded.antall_plasser
            returning *
        """.trimIndent()

        val queryResult = queryOf(
            query,
            tiltak.navn,
            tiltak.arrangorId,
            tiltak.tiltakskode,
            tiltak.id,
            tiltak.fraDato,
            tiltak.tilDato,
            tiltak.sakId,
            tiltak.apentForInnsok,
            tiltak.antallPlasser,
        ).map { DatabaseMapper.toAdapterTiltaksgjennomforing(it) }.asSingle
        return db.run(queryResult)!!
    }

    fun upsertDeltaker(deltaker: AdapterTiltakdeltaker): AdapterTiltakdeltaker {
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
            deltaker.id,
            deltaker.tiltaksgjennomforingId,
            deltaker.personId,
            deltaker.fraDato,
            deltaker.tilDato,
            deltaker.status.name
        ).map { DatabaseMapper.toAdapterTiltakdeltaker(it) }.asSingle
        return db.run(queryResult)!!
    }

    fun updateTiltaksgjennomforingWithSak(sak: AdapterSak): AdapterTiltaksgjennomforing? {
        logger.info("Oppdaterer tiltak med sak sakId={} tiltaksnummer={}", sak.id, sak.lopenummer)

        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing set tiltaksnummer = ?, aar = ? where sak_id = ? returning *
        """.trimIndent()

        val queryResult = queryOf(
            query,
            sak.lopenummer,
            sak.aar,
            sak.id,
        ).map { DatabaseMapper.toAdapterTiltaksgjennomforing(it) }.asSingle
        return db.run(queryResult)
    }
}
