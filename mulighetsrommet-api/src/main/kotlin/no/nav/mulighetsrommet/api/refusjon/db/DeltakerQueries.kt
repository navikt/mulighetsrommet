package no.nav.mulighetsrommet.api.refusjon.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.refusjon.model.DeltakerDto
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import org.intellij.lang.annotations.Language
import java.util.*

class DeltakerQueries(private val session: Session) {

    fun upsert(deltaker: DeltakerDbo) = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into deltaker (id,
                                  gjennomforing_id,
                                  start_dato,
                                  slutt_dato,
                                  registrert_tidspunkt,
                                  endret_tidspunkt,
                                  deltakelsesprosent,
                                  status_type,
                                  status_aarsak,
                                  status_opprettet_tidspunkt)
            values (:id::uuid,
                    :gjennomforing_id::uuid,
                    :start_dato,
                    :slutt_dato,
                    :registrert_tidspunkt,
                    :endret_tidspunkt,
                    :deltakelsesprosent,
                    :status_type::deltaker_status_type,
                    :status_aarsak::deltaker_status_aarsak,
                    :status_opprettet_tidspunkt)
            on conflict (id)
                do update set gjennomforing_id           = excluded.gjennomforing_id,
                              start_dato                 = excluded.start_dato,
                              slutt_dato                 = excluded.slutt_dato,
                              registrert_tidspunkt       = excluded.registrert_tidspunkt,
                              endret_tidspunkt           = excluded.endret_tidspunkt,
                              deltakelsesprosent         = excluded.deltakelsesprosent,
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
            "deltakelsesprosent" to deltaker.deltakelsesprosent,
            "status_type" to deltaker.status.type.name,
            "status_aarsak" to deltaker.status.aarsak?.name,
            "status_opprettet_tidspunkt" to deltaker.status.opprettetDato,
        )

        execute(queryOf(query, params))
    }

    fun setNorskIdent(deltakerId: UUID, norskIdent: NorskIdent) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update deltaker
            set norsk_ident = :norsk_ident
            where id = :deltaker_id::uuid
        """.trimIndent()

        val params = mapOf(
            "deltaker_id" to deltakerId,
            "norsk_ident" to norskIdent.value,
        )

        execute(queryOf(query, params))
    }

    fun get(id: UUID): DeltakerDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select id,
                   gjennomforing_id,
                   norsk_ident,
                   start_dato,
                   slutt_dato,
                   registrert_tidspunkt,
                   endret_tidspunkt,
                   deltakelsesprosent,
                   status_type,
                   status_aarsak,
                   status_opprettet_tidspunkt
            from deltaker
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toDeltakerDto() }
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
        gjennomforingId: UUID? = null,
    ): List<DeltakerDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select id,
                   gjennomforing_id,
                   norsk_ident,
                   start_dato,
                   slutt_dato,
                   registrert_tidspunkt,
                   endret_tidspunkt,
                   deltakelsesprosent,
                   status_type,
                   status_aarsak,
                   status_opprettet_tidspunkt
            from deltaker
            where :gjennomforing_id::uuid is null or gjennomforing_id = :gjennomforing_id::uuid
            limit :limit
            offset :offset
        """.trimIndent()

        val params = mapOf("gjennomforing_id" to gjennomforingId)

        queryOf(query, params + pagination.parameters)
            .map { it.toDeltakerDto() }
            .asList
            .runWithSession(session)
    }

    fun delete(id: UUID) = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete
            from deltaker
            where id = ?::uuid
        """.trimIndent()

        execute(queryOf(query, id))
    }
}

private fun Row.toDeltakerDto() = DeltakerDto(
    id = uuid("id"),
    gjennomforingId = uuid("gjennomforing_id"),
    norskIdent = stringOrNull("norsk_ident")?.let { NorskIdent(it) },
    startDato = localDateOrNull("start_dato"),
    sluttDato = localDateOrNull("slutt_dato"),
    registrertTidspunkt = localDateTime("registrert_tidspunkt"),
    endretTidspunkt = localDateTime("endret_tidspunkt"),
    deltakelsesprosent = doubleOrNull("deltakelsesprosent"),
    status = DeltakerStatus(
        type = DeltakerStatus.Type.valueOf(string("status_type")),
        aarsak = stringOrNull("status_aarsak")?.let { DeltakerStatus.Aarsak.valueOf(it) },
        opprettetDato = localDateTime("status_opprettet_tidspunkt"),
    ),
)
