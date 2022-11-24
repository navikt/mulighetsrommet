package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.models.Deltaker
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class DeltakerRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun save(deltaker: Deltaker): QueryResult<Deltaker> = query {
        logger.info("Lagrer deltaker id=${deltaker.id}")

        @Language("PostgreSQL")
        val query = """
            insert into deltaker (id, tiltaksgjennomforing_id, fnr, status, fra_dato, til_dato, virksomhetsnr)
            values (?, ?, ?, ?::deltakerstatus, ?, ?, ?)
            on conflict (id)
            do update set
                tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                fnr = excluded.fnr,
                status = excluded.status,
                fra_dato = excluded.fra_dato,
                til_dato = excluded.til_dato,
                virksomhetsnr = excluded.virksomhetsnr
            returning *
        """.trimIndent()

        deltaker.run {
            queryOf(
                query,
                id,
                tiltaksgjennomforingId,
                fnr,
                status.name,
                fraDato,
                tilDato,
                virksomhetsnr
            )
        }
            .map { DatabaseMapper.toDeltaker(it) }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
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
