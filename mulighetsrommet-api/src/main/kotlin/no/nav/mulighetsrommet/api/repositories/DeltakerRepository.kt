package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class DeltakerRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(deltaker: DeltakerDbo): DeltakerDbo {
        logger.info("Lagrer deltaker id=${deltaker.id}")

        @Language("PostgreSQL")
        val query = """
            insert into deltaker (id, tiltaksgjennomforing_id, norsk_ident, status, start_dato, slutt_dato, registrert_dato)
            values (:id::uuid, :tiltaksgjennomforing_id::uuid, :norsk_ident, :status::deltakerstatus, :start_dato, :slutt_dato, :registrert_dato)
            on conflict (id)
                do update set tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                              status                  = excluded.status,
                              norsk_ident             = excluded.norsk_ident,
                              start_dato              = excluded.start_dato,
                              slutt_dato              = excluded.slutt_dato,
                              registrert_dato         = excluded.registrert_dato
            returning *
        """.trimIndent()

        return queryOf(query, deltaker.toSqlParameters())
            .map { it.toDeltakerDbo() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getAll(tiltaksgjennomforingId: UUID? = null): List<DeltakerDbo> {
        val where = DatabaseUtils.andWhereParameterNotNull(
            tiltaksgjennomforingId to "tiltaksgjennomforing_id = :tiltaksgjennomforing_id"
        )

        @Language("PostgreSQL")
        val query = """
            select id, tiltaksgjennomforing_id, norsk_ident, status, start_dato, slutt_dato, registrert_dato
            from deltaker
            $where
        """.trimIndent()

        return queryOf(query, mapOf("tiltaksgjennomforing_id" to tiltaksgjennomforingId))
            .map { it.toDeltakerDbo() }
            .asList
            .let { db.run(it) }
    }

    fun delete(id: UUID) {
        logger.info("Sletter deltaker id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from deltaker
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    private fun DeltakerDbo.toSqlParameters() = mapOf(
        "id" to id,
        "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
        "norsk_ident" to norskIdent,
        "status" to status.name,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "registrert_dato" to registrertDato,
    )

    private fun Row.toDeltakerDbo() = DeltakerDbo(
        id = uuid("id"),
        tiltaksgjennomforingId = uuid("tiltaksgjennomforing_id"),
        norskIdent = string("norsk_ident"),
        status = Deltakerstatus.valueOf(string("status")),
        startDato = localDateOrNull("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        registrertDato = localDateTime("registrert_dato"),
    )
}
