package no.nav.mulighetsrommet.api.persistence.deltaker

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerRepository
import no.nav.mulighetsrommet.api.domain.deltaker.NavVeileder
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusAarsakType
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.util.UUID

class DeltakerQueries(private val session: Session) : DeltakerRepository {
    override fun save(deltaker: Deltaker): Unit = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into deltaker (id,
                                  gjennomforing_id,
                                  start_dato,
                                  slutt_dato,
                                  registrert_tidspunkt,
                                  endret_tidspunkt,
                                  status_type,
                                  status_aarsak,
                                  status_opprettet_tidspunkt,
                                  innhold_annet,
                                  nav_veileder_nav_ident,
                                  nav_veileder_enhetsnummer)
            values (:id::uuid,
                    :gjennomforing_id::uuid,
                    :start_dato,
                    :slutt_dato,
                    :registrert_tidspunkt,
                    :endret_tidspunkt,
                    :status_type::deltaker_status_type,
                    :status_aarsak::deltaker_status_aarsak,
                    :status_opprettet_tidspunkt,
                    :innhold_annet,
                    :nav_veileder_nav_ident,
                    :nav_veileder_enhetsnummer)
            on conflict (id)
                do update set gjennomforing_id           = excluded.gjennomforing_id,
                              start_dato                 = excluded.start_dato,
                              slutt_dato                 = excluded.slutt_dato,
                              registrert_tidspunkt       = excluded.registrert_tidspunkt,
                              endret_tidspunkt           = excluded.endret_tidspunkt,
                              status_type                = excluded.status_type,
                              status_aarsak              = excluded.status_aarsak,
                              status_opprettet_tidspunkt = excluded.status_opprettet_tidspunkt,
                              innhold_annet              = excluded.innhold_annet,
                              nav_veileder_nav_ident     = excluded.nav_veileder_nav_ident,
                              nav_veileder_enhetsnummer  = excluded.nav_veileder_enhetsnummer
        """.trimIndent()
        val params = mapOf(
            "id" to deltaker.id,
            "gjennomforing_id" to deltaker.gjennomforingId,
            "start_dato" to deltaker.startDato,
            "slutt_dato" to deltaker.sluttDato,
            "registrert_tidspunkt" to deltaker.registrertTidspunkt,
            "endret_tidspunkt" to deltaker.endretTidspunkt,
            "status_type" to deltaker.status.type.name,
            "status_aarsak" to deltaker.status.aarsak?.name,
            "status_opprettet_tidspunkt" to deltaker.status.opprettetTidspunkt,
            "innhold_annet" to deltaker.innholdAnnet,
            "nav_veileder_nav_ident" to deltaker.navVeileder?.navIdent?.value,
            "nav_veileder_enhetsnummer" to deltaker.navVeileder?.enhetsnummer?.value,
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

    override fun get(id: UUID): Deltaker? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_deltaker
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toDeltaker() }
    }

    override fun getByGjennomforing(gjennomforingId: UUID): List<Deltaker> {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_deltaker
            where gjennomforing_id = ?::uuid
        """.trimIndent()

        return session.list(queryOf(query, gjennomforingId)) { it.toDeltaker() }
    }

    override fun delete(id: UUID) {
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
    startDato = localDateOrNull("start_dato"),
    sluttDato = localDateOrNull("slutt_dato"),
    registrertTidspunkt = localDateTime("registrert_tidspunkt"),
    endretTidspunkt = localDateTime("endret_tidspunkt"),
    status = DeltakerStatus(
        type = DeltakerStatusType.valueOf(string("status_type")),
        aarsak = stringOrNull("status_aarsak")?.let { DeltakerStatusAarsakType.valueOf(it) },
        opprettetTidspunkt = localDateTime("status_opprettet_tidspunkt"),
    ),
    deltakelsesmengder = stringOrNull("deltakelsesmengder_json")?.let { Json.decodeFromString(it) } ?: listOf(),
    innholdAnnet = stringOrNull("innhold_annet"),
    navVeileder = stringOrNull("nav_veileder_nav_ident")?.let {
        NavVeileder(
            navIdent = NavIdent(it),
            enhetsnummer = stringOrNull("nav_veileder_enhetsnummer")?.let { enhet -> NavEnhetNummer(enhet) },
        )
    },
)
