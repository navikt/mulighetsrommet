package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import org.intellij.lang.annotations.Language
import java.util.*

class DeltakerRepository(private val db: Database) {
    fun upsert(deltaker: DeltakerDbo) = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
            insert into deltaker (id,
                                  gjennomforing_id,
                                  start_dato,
                                  slutt_dato,
                                  registrert_tidspunkt,
                                  endret_tidspunkt,
                                  stillingsprosent,
                                  status_type,
                                  status_aarsak,
                                  status_opprettet_tidspunkt)
            values (:id::uuid,
                    :gjennomforing_id::uuid,
                    :start_dato,
                    :slutt_dato,
                    :registrert_tidspunkt,
                    :endret_tidspunkt,
                    :stillingsprosent,
                    :status_type::deltaker_status_type,
                    :status_aarsak::deltaker_status_aarsak,
                    :status_opprettet_tidspunkt)
            on conflict (id)
                do update set gjennomforing_id           = excluded.gjennomforing_id,
                              start_dato                 = excluded.start_dato,
                              slutt_dato                 = excluded.slutt_dato,
                              registrert_tidspunkt       = excluded.registrert_tidspunkt,
                              endret_tidspunkt           = excluded.endret_tidspunkt,
                              stillingsprosent           = excluded.stillingsprosent,
                              status_type                = excluded.status_type,
                              status_aarsak              = excluded.status_aarsak,
                              status_opprettet_tidspunkt = excluded.status_opprettet_tidspunkt
        """.trimIndent()

        val params = mapOf(
            "id" to deltaker.id,
            "gjennomforing_id" to deltaker.gjennomforingId,
            "start_dato" to deltaker.startDato,
            "slutt_dato" to deltaker.sluttDato,
            "registrert_tidspunkt" to deltaker.registrertTidspunkt,
            "endret_tidspunkt" to deltaker.endretTidspunkt,
            "stillingsprosent" to deltaker.stillingsprosent,
            "status_type" to deltaker.status.type.name,
            "status_aarsak" to deltaker.status.aarsak?.name,
            "status_opprettet_tidspunkt" to deltaker.status.opprettetDato,
        )

        queryOf(query, params).asExecute.runWithSession(session)
    }

    fun getAll(tiltaksgjennomforingId: UUID? = null): List<DeltakerDbo> = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
            select id,
                   gjennomforing_id,
                   start_dato,
                   slutt_dato,
                   registrert_tidspunkt,
                   endret_tidspunkt,
                   stillingsprosent,
                   status_type,
                   status_aarsak,
                   status_opprettet_tidspunkt
            from deltaker
            where :gjennomforing_id::uuid is null or gjennomforing_id = :gjennomforing_id::uuid
        """.trimIndent()

        val params = mapOf("gjennomforing_id" to tiltaksgjennomforingId)

        queryOf(query, params)
            .map { it.toDeltakerDbo() }
            .asList
            .runWithSession(session)
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from deltaker
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    private fun Row.toDeltakerDbo() = DeltakerDbo(
        id = uuid("id"),
        gjennomforingId = uuid("gjennomforing_id"),
        startDato = localDateOrNull("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        registrertTidspunkt = localDateTime("registrert_tidspunkt"),
        endretTidspunkt = localDateTime("endret_tidspunkt"),
        stillingsprosent = doubleOrNull("stillingsprosent"),
        status = AmtDeltakerStatus(
            type = AmtDeltakerStatus.Type.valueOf(string("status_type")),
            aarsak = stringOrNull("status_aarsak")?.let { AmtDeltakerStatus.Aarsak.valueOf(it) },
            opprettetDato = localDateTime("status_opprettet_tidspunkt"),
        ),

    )
}
