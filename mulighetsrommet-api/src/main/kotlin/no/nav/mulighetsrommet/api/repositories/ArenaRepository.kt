package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class ArenaRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsertTiltakstype(tiltakstype: AdapterTiltak): QueryResult<AdapterTiltak> = query {
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

        tiltakstype.run { queryOf(query, navn, innsatsgruppe, tiltakskode, fraDato, tilDato) }
            .map { DatabaseMapper.toAdapterTiltak(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun deleteTiltakstype(tiltakstype: AdapterTiltak): QueryResult<Unit> = query {
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

    fun upsertTiltaksgjennomforing(
        tiltak: AdapterTiltaksgjennomforing
    ): QueryResult<AdapterTiltaksgjennomforing> = query {
        logger.info("Lagrer tiltak id=${tiltak.tiltaksgjennomforingId}, tiltakskode=${tiltak.tiltakskode}, sakId=${tiltak.sakId}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (navn,
                                              arrangor_id,
                                              tiltakskode,
                                              arena_id,
                                              fra_dato,
                                              til_dato,
                                              sak_id,
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

        tiltak
            .run {
                queryOf(
                    query,
                    navn,
                    arrangorId,
                    tiltakskode,
                    tiltaksgjennomforingId,
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

    fun deleteTiltaksgjennomforing(tiltak: AdapterTiltaksgjennomforing): QueryResult<Unit> = query {
        logger.info("Sletter tiltak id=${tiltak.tiltaksgjennomforingId}, tiltakskode=${tiltak.tiltakskode}, sakId=${tiltak.sakId}")

        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing
            where arena_id = ?
        """.trimIndent()

        query {
            tiltak.run { queryOf(query, tiltaksgjennomforingId) }
                .asExecute
                .let { db.run(it) }
        }
    }

    fun upsertDeltaker(deltaker: AdapterTiltakdeltaker): QueryResult<AdapterTiltakdeltaker> = query {
        logger.info("Lagrer deltaker id=${deltaker.tiltaksdeltakerId}, tiltak=${deltaker.tiltaksgjennomforingId}")

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

        deltaker.run {
            queryOf(
                query,
                tiltaksdeltakerId,
                tiltaksgjennomforingId,
                personId,
                fraDato,
                tilDato,
                status.name
            )
        }
            .map { DatabaseMapper.toAdapterTiltakdeltaker(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun deleteDeltaker(deltaker: AdapterTiltakdeltaker): QueryResult<Unit> = query {
        logger.info("Sletter deltaker id=${deltaker.tiltaksdeltakerId}, tiltak=${deltaker.tiltaksgjennomforingId}")

        @Language("PostgreSQL")
        val query = """
            delete from deltaker
            where arena_id = ?
        """.trimIndent()

        deltaker.run { queryOf(query, tiltaksdeltakerId) }
            .asExecute
            .let { db.run(it) }
    }

    fun updateTiltaksgjennomforingWithSak(sak: AdapterSak): QueryResult<AdapterTiltaksgjennomforing?> = query {
        logger.info("Oppdaterer tiltak med sak sakId=${sak.sakId} tiltaksnummer=${sak.lopenummer}")

        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing set tiltaksnummer = ?, aar = ?
            where sak_id = ?
            returning *
        """.trimIndent()

        sak.run { queryOf(query, lopenummer, aar, sakId) }
            .map { DatabaseMapper.toAdapterTiltaksgjennomforing(it) }
            .asSingle
            .let { db.run(it) }
    }

    fun unsetSakOnTiltaksgjennomforing(sak: AdapterSak): QueryResult<AdapterTiltaksgjennomforing?> = query {
        logger.info("Fjerner referanse til sak for tiltak sakId=${sak.sakId} tiltaksnummer=${sak.lopenummer}")

        @Language("PostgreSQL")
        val query = """
            update tiltaksgjennomforing set tiltaksnummer = null, aar = null
            where sak_id = ?
            returning *
        """.trimIndent()

        sak.run { queryOf(query, sakId) }
            .map { DatabaseMapper.toAdapterTiltaksgjennomforing(it) }
            .asSingle
            .let { db.run(it) }
    }
}
