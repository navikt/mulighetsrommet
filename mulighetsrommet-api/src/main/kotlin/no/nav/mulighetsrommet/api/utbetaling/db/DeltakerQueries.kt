package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utbetaling.model.Deltakelsesmengde
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import org.intellij.lang.annotations.Language
import java.util.*

class DeltakerQueries(private val session: Session) {

    fun upsert(deltaker: DeltakerDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into deltaker (id,
                                  gjennomforing_id,
                                  start_dato,
                                  slutt_dato,
                                  registrert_dato,
                                  endret_tidspunkt,
                                  deltakelsesprosent,
                                  status_type,
                                  status_aarsak,
                                  status_opprettet_tidspunkt)
            values (:id::uuid,
                    :gjennomforing_id::uuid,
                    :start_dato,
                    :slutt_dato,
                    :registrert_dato,
                    :endret_tidspunkt,
                    :deltakelsesprosent,
                    :status_type::deltaker_status_type,
                    :status_aarsak::deltaker_status_aarsak,
                    :status_opprettet_tidspunkt)
            on conflict (id)
                do update set gjennomforing_id           = excluded.gjennomforing_id,
                              start_dato                 = excluded.start_dato,
                              slutt_dato                 = excluded.slutt_dato,
                              registrert_dato            = excluded.registrert_dato,
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
            "registrert_dato" to deltaker.registrertDato,
            "endret_tidspunkt" to deltaker.endretTidspunkt,
            "deltakelsesprosent" to deltaker.deltakelsesprosent,
            "status_type" to deltaker.status.type.name,
            "status_aarsak" to deltaker.status.aarsak?.name,
            "status_opprettet_tidspunkt" to deltaker.status.opprettetDato,
        )
        execute(queryOf(query, params))

        @Language("PostgreSQL")
        val deleteDeltakelsesmengderQuery = """
            delete from deltaker_deltakelsesmengde where deltaker_id = ?::uuid;
        """.trimIndent()
        execute(queryOf(deleteDeltakelsesmengderQuery, deltaker.id))

        @Language("PostgreSQL")
        val insertDeltakelsesmengdeQuery = """
            insert into deltaker_deltakelsesmengde (deltaker_id, gyldig_fra, opprettet_tidspunkt, deltakelsesprosent)
            values (:deltaker_id::uuid, :gyldig_fra, :opprettet_tidspunkt, :deltakelsesprosent)
        """.trimIndent()
        val deltakelsesmengder = deltaker.deltakelsesmengder.map {
            mapOf(
                "deltaker_id" to deltaker.id,
                "gyldig_fra" to it.gyldigFra,
                "opprettet_tidspunkt" to it.opprettetTidspunkt,
                "deltakelsesprosent" to it.deltakelsesprosent,
            )
        }
        batchPreparedNamedStatement(insertDeltakelsesmengdeQuery, deltakelsesmengder)
    }

    fun setNorskIdent(deltakerId: UUID, norskIdent: NorskIdent): Boolean {
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

        return session.execute(queryOf(query, params))
    }

    fun get(id: UUID): Deltaker? {
        @Language("PostgreSQL")
        val query = """
            select id,
                   gjennomforing_id,
                   norsk_ident,
                   start_dato,
                   slutt_dato,
                   registrert_dato,
                   endret_tidspunkt,
                   deltakelsesprosent,
                   status_type,
                   status_aarsak,
                   status_opprettet_tidspunkt
            from deltaker
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toDeltaker() }
    }

    fun getDeltakelsesmengder(id: UUID): List<Deltakelsesmengde> {
        @Language("PostgreSQL")
        val query = """
            select gyldig_fra, deltakelsesprosent
            from deltaker_deltakelsesmengde
            where deltaker_id = ?::uuid
            order by gyldig_fra
        """.trimIndent()

        return session.list(queryOf(query, id)) {
            Deltakelsesmengde(
                gyldigFra = it.localDate("gyldig_fra"),
                deltakelsesprosent = it.double("deltakelsesprosent"),
            )
        }
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
        gjennomforingId: UUID? = null,
    ): List<Deltaker> {
        @Language("PostgreSQL")
        val query = """
            select id,
                   gjennomforing_id,
                   norsk_ident,
                   start_dato,
                   slutt_dato,
                   registrert_dato,
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

        return session.list(queryOf(query, params + pagination.parameters)) { it.toDeltaker() }
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete
            from deltaker
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }
}

private fun Row.toDeltaker() = Deltaker(
    id = uuid("id"),
    gjennomforingId = uuid("gjennomforing_id"),
    norskIdent = stringOrNull("norsk_ident")?.let { NorskIdent(it) },
    startDato = localDateOrNull("start_dato"),
    sluttDato = localDateOrNull("slutt_dato"),
    registrertDato = localDate("registrert_dato"),
    endretTidspunkt = localDateTime("endret_tidspunkt"),
    deltakelsesprosent = doubleOrNull("deltakelsesprosent"),
    status = DeltakerStatus(
        type = DeltakerStatus.Type.valueOf(string("status_type")),
        aarsak = stringOrNull("status_aarsak")?.let { DeltakerStatus.Aarsak.valueOf(it) },
        opprettetDato = localDateTime("status_opprettet_tidspunkt"),
    ),
)
