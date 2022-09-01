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
        logger.info("Lagrer tiltakstype tiltakskode=${tiltakstype.tiltakskode}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (navn, innsatsgruppe_id, tiltakskode, fra_dato, til_dato)
            values (?, ?, ?, ?, ?)
            on conflict (tiltakskode)
                do update set navn             = excluded.navn,
                              innsatsgruppe_id = excluded.innsatsgruppe_id,
                              tiltakskode      = excluded.tiltakskode,
                              fra_dato         = excluded.fra_dato,
                              til_dato         = excluded.til_dato
            returning *
        """.trimIndent()

        return tiltakstype.run { queryOf(query, navn, innsatsgruppe, tiltakskode, fraDato, tilDato) }
            .map { DatabaseMapper.toAdapterTiltak(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun deleteTiltakstype(tiltakstype: AdapterTiltak) {
        logger.info("Sletter tiltakstype tiltakskode=${tiltakstype.tiltakskode}")

        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where tiltakskode = ?
        """.trimIndent()

        tiltakstype.run { queryOf(query, tiltakskode) }
            .asExecute
            .let { db.run(it) }
    }

    fun upsertTiltaksgjennomforing(tiltak: AdapterTiltaksgjennomforing): AdapterTiltaksgjennomforing {
        logger.info("Lagrer tiltak id=${tiltak.id}, tiltakskode=${tiltak.tiltakskode}, sakId=${tiltak.sakId}")

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

        return tiltak
            .run {
                queryOf(
                    query,
                    navn,
                    arrangorId,
                    tiltakskode,
                    id,
                    fraDato,
                    tilDato,
                    sakId,
                    apentForInnsok,
                    antallPlasser
                )
            }
            .map { DatabaseMapper.toAdapterTiltaksgjennomforing(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun deleteTiltaksgjennomforing(tiltak: AdapterTiltaksgjennomforing) {
        logger.info("Sletter tiltak id=${tiltak.id}, tiltakskode=${tiltak.tiltakskode}, sakId=${tiltak.sakId}")

        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing
            where arena_id = ?
        """.trimIndent()

        tiltak.run { queryOf(query, id) }
            .asExecute
            .let { db.run(it) }
    }

    fun upsertDeltaker(deltaker: AdapterTiltakdeltaker): AdapterTiltakdeltaker {
        logger.info("Lagrer deltaker id=${deltaker.id}, tiltak=${deltaker.tiltaksgjennomforingId}")

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

        return deltaker.run { queryOf(query, id, tiltaksgjennomforingId, personId, fraDato, tilDato, status.name) }
            .map { DatabaseMapper.toAdapterTiltakdeltaker(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun deleteDeltaker(deltaker: AdapterTiltakdeltaker) {
        logger.info("Sletter deltaker id=${deltaker.id}, tiltak=${deltaker.tiltaksgjennomforingId}")

        @Language("PostgreSQL")
        val query = """
            delete from deltaker
            where arena_id = ?
        """.trimIndent()

        deltaker.run { queryOf(query, id) }
            .asExecute
            .let { db.run(it) }
    }

    fun updateTiltaksgjennomforingWithSak(sak: AdapterSak): AdapterTiltaksgjennomforing? {
        logger.info("Oppdaterer tiltak med sak sakId=${sak.id} tiltaksnummer=${sak.lopenummer}")

        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing set tiltaksnummer = ?, aar = ?
            where sak_id = ?
            returning *
        """.trimIndent()

        return sak.run { queryOf(query, lopenummer, aar, id) }
            .map { DatabaseMapper.toAdapterTiltaksgjennomforing(it) }
            .asSingle
            .let { db.run(it) }
    }

    fun unsetSakOnTiltaksgjennomforing(sak: AdapterSak): AdapterTiltaksgjennomforing? {
        logger.info("Fjerner referanse til sak for tiltak sakId=${sak.id} tiltaksnummer=${sak.lopenummer}")

        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing set tiltaksnummer = null, aar = null
            where sak_id = ?
            returning *
        """.trimIndent()

        return sak.run { queryOf(query, id) }
            .map { DatabaseMapper.toAdapterTiltaksgjennomforing(it) }
            .asSingle
            .let { db.run(it) }
    }
}
