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
import no.nav.mulighetsrommet.domain.models.Deltaker
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class ArenaRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsertTiltakstype(tiltakstype: Tiltakstype): QueryResult<Tiltakstype> = query {
        logger.info("Lagrer tiltakstype id=${tiltakstype.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (id, navn, tiltakskode)
            values (?, ?, ?)
            on conflict (id)
                do update set navn             = excluded.navn,
                              tiltakskode      = excluded.tiltakskode
            returning *
        """.trimIndent()

        tiltakstype.run { queryOf(query, id, navn, tiltakskode) }
            .map { DatabaseMapper.toTiltakstype(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun deleteTiltakstype(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter tiltakstype id=${id}")

        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where id = ?
        """.trimIndent()

        run { queryOf(query, id) }
            .asExecute
            .let { db.run(it) }
    }

    fun upsertTiltaksgjennomforing(
        tiltaksgjennomforing: Tiltaksgjennomforing
    ): QueryResult<Tiltaksgjennomforing> = query {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltaksgjennomforing.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (id, navn, tiltakstype_id, tiltaksnummer)
            values (?, ?, ?, ?)
            on conflict (id)
                do update set navn             = excluded.navn,
                              tiltakstype_id      = excluded.tiltakstype_id,
                              tiltaksnummer      = excluded.tiltaksnummer
            returning *
        """.trimIndent()

        tiltaksgjennomforing
            .run {
                queryOf(
                    query,
                    id,
                    navn,
                    tiltakstypeId,
                    tiltaksnummer
                )
            }
            .map { DatabaseMapper.toTiltaksgjennomforing(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun deleteTiltaksgjennomforing(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter tiltaksgjennomføring id=${id}")

        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing
            where id = ?
        """.trimIndent()

        query {
            run { queryOf(query, id) }
                .asExecute
                .let { db.run(it) }
        }
    }

    fun upsertDeltaker(deltaker: Deltaker): QueryResult<Deltaker> = query {
        logger.info("Lagrer deltaker id=${deltaker.id}")

        @Language("PostgreSQL")
        val query = """
            insert into deltaker (id, tiltaksgjennomforing_id, fnr, status)
            values (?, ?, ?, ?::deltakerstatus)
            on conflict (id)
            do update set
                tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                fnr = excluded.fnr,
                status = excluded.status
            returning *
        """.trimIndent()

        deltaker.run {
            queryOf(
                query,
                id,
                tiltaksgjennomforingId,
                fnr,
                status.name
            )
        }
            .map { DatabaseMapper.toDeltaker(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun deleteDeltaker(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter deltaker id=${id}")

        @Language("PostgreSQL")
        val query = """
            delete from deltaker
            where id = ?
        """.trimIndent()

        run { queryOf(query, id) }
            .asExecute
            .let { db.run(it) }
    }
}
