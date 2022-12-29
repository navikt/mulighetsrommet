package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class DeltakerRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(deltaker: DeltakerDbo): QueryResult<DeltakerDbo> = query {
        logger.info("Lagrer deltaker id=${deltaker.id}")

        @Language("PostgreSQL")
        val query = """
            insert into deltaker (id, tiltaksgjennomforing_id, norsk_ident, status, fra_dato, til_dato)
            values (:id::uuid, :tiltaksgjennomforing_id::uuid, :norsk_ident, :status::deltakerstatus, :fra_dato, :til_dato)
            on conflict (id)
                do update set tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                              norsk_ident             = excluded.norsk_ident,
                              status                  = excluded.status,
                              fra_dato                = excluded.fra_dato,
                              til_dato                = excluded.til_dato
            returning *
        """.trimIndent()

        queryOf(query, deltaker.toSqlParameters())
            .map { it.toDeltakerDbo() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter deltaker id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from deltaker
            where id = ?::uuid
        """.trimIndent()

        run { queryOf(query, id) }
            .asExecute
            .let { db.run(it) }
    }

    private fun DeltakerDbo.toSqlParameters() = mapOf(
        "id" to id,
        "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
        "norsk_ident" to norskIdent,
        "status" to status.name,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
    )

    private fun Row.toDeltakerDbo() = DeltakerDbo(
        id = uuid("id"),
        tiltaksgjennomforingId = uuid("tiltaksgjennomforing_id"),
        norskIdent = string("norsk_ident"),
        status = Deltakerstatus.valueOf(string("status")),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
    )
}
