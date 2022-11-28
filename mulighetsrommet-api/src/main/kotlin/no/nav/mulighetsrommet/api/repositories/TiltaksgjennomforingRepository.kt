package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun save(
        tiltaksgjennomforing: Tiltaksgjennomforing
    ): QueryResult<Tiltaksgjennomforing> = query {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltaksgjennomforing.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (id, navn, tiltakstype_id, tiltaksnummer, virksomhetsnr)
            values (?::uuid, ?, ?::uuid, ?, ?)
            on conflict (id)
                do update set navn             = excluded.navn,
                              tiltakstype_id      = excluded.tiltakstype_id,
                              tiltaksnummer      = excluded.tiltaksnummer,
                              virksomhetsnr     = excluded.virksomhetsnr
            returning *
        """.trimIndent()

        tiltaksgjennomforing
            .run {
                queryOf(
                    query,
                    id,
                    navn,
                    tiltakstypeId,
                    tiltaksnummer,
                    virksomhetsnr
                )
            }
            .map { DatabaseMapper.toTiltaksgjennomforing(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter tiltaksgjennomføring id=$id")

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
}
