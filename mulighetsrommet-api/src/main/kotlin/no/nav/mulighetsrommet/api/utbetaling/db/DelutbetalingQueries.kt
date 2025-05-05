package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tiltakstype.db.createArrayOfTiltakskode
import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.utils.periode
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.oppgaver.OppgaveTiltakstype
import no.nav.tiltak.okonomi.FakturaStatusType
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class DelutbetalingQueries(private val session: Session) {
    fun upsert(delutbetaling: DelutbetalingDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into delutbetaling (
                id,
                tilsagn_id,
                utbetaling_id,
                status,
                belop,
                gjor_opp_tilsagn,
                periode,
                lopenummer,
                fakturanummer,
                faktura_status,
                faktura_status_sist_oppdatert
            ) values (
                :id::uuid,
                :tilsagn_id::uuid,
                :utbetaling_id::uuid,
                :status::delutbetaling_status,
                :belop,
                :gjor_opp_tilsagn::boolean,
                :periode::daterange,
                :lopenummer,
                :fakturanummer,
                :faktura_status,
                :faktura_status_sist_oppdatert::date
            ) on conflict (utbetaling_id, tilsagn_id) do update set
                status                  = excluded.status,
                belop                   = excluded.belop,
                gjor_opp_tilsagn        = excluded.gjor_opp_tilsagn,
                periode                 = delutbetaling.periode,
                lopenummer              = delutbetaling.lopenummer,
                fakturanummer           = delutbetaling.fakturanummer,
                faktura_status          = delutbetaling.faktura_status,
                faktura_status_sist_oppdatert   = excluded.faktura_status_sist_oppdatert
        """.trimIndent()

        val params = mapOf(
            "id" to delutbetaling.id,
            "tilsagn_id" to delutbetaling.tilsagnId,
            "utbetaling_id" to delutbetaling.utbetalingId,
            "status" to delutbetaling.status.name,
            "faktura_status_sist_oppdatert" to delutbetaling.fakturaStatusSistOppdatert,
            "belop" to delutbetaling.belop,
            "gjor_opp_tilsagn" to delutbetaling.gjorOppTilsagn,
            "periode" to delutbetaling.periode.toDaterange(),
            "lopenummer" to delutbetaling.lopenummer,
            "fakturanummer" to delutbetaling.fakturanummer,
            "faktura_status" to delutbetaling.fakturaStatus?.name,
        )

        session.execute(queryOf(query, params))
    }

    fun getNextLopenummerByTilsagn(tilsagnId: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            select coalesce(max(lopenummer), 0) + 1 as lopenummer
            from delutbetaling
            where tilsagn_id = ?
        """.trimIndent()

        return session.requireSingle(queryOf(query, tilsagnId)) { it.int("lopenummer") }
    }

    fun getSkalSendesTilOkonomi(tilsagnId: UUID): List<Delutbetaling> {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                tilsagn_id,
                utbetaling_id,
                status,
                belop,
                gjor_opp_tilsagn,
                periode,
                lopenummer,
                fakturanummer,
                faktura_status,
                faktura_status_sist_oppdatert
            from delutbetaling
            where
                tilsagn_id = :tilsagn_id
                and sendt_til_okonomi_tidspunkt is null
        """.trimIndent()

        return session.list(queryOf(query, mapOf("tilsagn_id" to tilsagnId))) { it.toDelutbetalingDto() }
    }

    fun setStatus(id: UUID, status: DelutbetalingStatus) {
        @Language("PostgreSQL")
        val query = """
            update delutbetaling
            set status = :status::delutbetaling_status
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "status" to status.name,
        )

        session.execute(queryOf(query, params))
    }

    fun setStatusForDelutbetalingerForBetaling(utbetalingId: UUID, status: DelutbetalingStatus) {
        @Language("PostgreSQL")
        val query = """
            update delutbetaling
            set status = :status::delutbetaling_status
            where utbetaling_id = :utbetalingId::uuid
        """.trimIndent()

        val params = mapOf(
            "utbetalingId" to utbetalingId,
            "status" to status.name,
        )

        session.execute(queryOf(query, params))
    }

    fun setSendtTilOkonomi(utbetalingId: UUID, tilsagnId: UUID, tidspunkt: LocalDateTime) {
        @Language("PostgreSQL")
        val query = """
            update delutbetaling set
                sendt_til_okonomi_tidspunkt = :tidspunkt
            where
                utbetaling_id = :utbetaling_id::uuid
                and tilsagn_id = :tilsagn_id::uuid
        """.trimIndent()

        val params = mapOf(
            "utbetaling_id" to utbetalingId,
            "tilsagn_id" to tilsagnId,
            "tidspunkt" to tidspunkt,
        )

        session.execute(queryOf(query, params))
    }

    fun getByUtbetalingId(id: UUID): List<Delutbetaling> {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                tilsagn_id,
                utbetaling_id,
                status,
                belop,
                gjor_opp_tilsagn,
                periode,
                lopenummer,
                fakturanummer,
                faktura_status,
                faktura_status_sist_oppdatert
            from delutbetaling
            where utbetaling_id = ?
            order by created_at desc
        """.trimIndent()

        return session.list(queryOf(query, id)) { it.toDelutbetalingDto() }
    }

    fun get(id: UUID): Delutbetaling? {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                tilsagn_id,
                utbetaling_id,
                status,
                belop,
                gjor_opp_tilsagn,
                periode,
                lopenummer,
                fakturanummer,
                faktura_status,
                faktura_status_sist_oppdatert
            from delutbetaling
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toDelutbetalingDto() }
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from delutbetaling where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    fun getOppgaveData(
        kostnadssteder: Set<NavEnhetNummer>?,
        tiltakskoder: Set<Tiltakskode>?,
    ): List<DelutbetalingOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            select
                delutbetaling.id,
                delutbetaling.tilsagn_id,
                delutbetaling.utbetaling_id,
                delutbetaling.status,
                delutbetaling.belop,
                delutbetaling.gjor_opp_tilsagn,
                delutbetaling.periode,
                delutbetaling.lopenummer,
                delutbetaling.fakturanummer,
                delutbetaling.faktura_status,
                delutbetaling.faktura_status_sist_oppdatert,
                tilsagn.gjennomforing_id,
                gjennomforing.navn,
                tiltakstype.tiltakskode,
                tiltakstype.navn as tiltakstype_navn
            from delutbetaling
                inner join tilsagn on tilsagn.id = delutbetaling.tilsagn_id
                inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
                inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
            where
                (:tiltakskoder::tiltakskode[] is null or tiltakstype.tiltakskode = any(:tiltakskoder::tiltakskode[])) and
                (:kostnadssteder::text[] is null or tilsagn.kostnadssted = any(:kostnadssteder))
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder?.let { session.createArrayOfTiltakskode(it) },
            "kostnadssteder" to kostnadssteder?.let { session.createArrayOfValue(it) { it.value } },
        )

        return session.list(queryOf(query, params)) {
            DelutbetalingOppgaveData(
                delutbetaling = it.toDelutbetalingDto(),
                gjennomforingId = it.uuid("gjennomforing_id"),
                gjennomforingsnavn = it.string("navn"),
                tiltakstype = OppgaveTiltakstype(
                    tiltakskode = Tiltakskode.valueOf(it.string("tiltakskode")),
                    navn = it.string("tiltakstype_navn"),
                ),
            )
        }
    }

    fun setFakturaStatus(fakturanummer: String, status: FakturaStatusType, fakturaStatusSistOppdatert: LocalDateTime?) {
        @Language("PostgreSQL")
        val query = """
            update delutbetaling
            set faktura_status = ?,
            faktura_status_sist_oppdatert = ?
            where fakturanummer = ?
        """.trimIndent()

        session.execute(queryOf(query, status.name, fakturaStatusSistOppdatert, fakturanummer))
    }
}

private fun Row.toDelutbetalingDto() = Delutbetaling(
    id = uuid("id"),
    tilsagnId = uuid("tilsagn_id"),
    utbetalingId = uuid("utbetaling_id"),
    belop = int("belop"),
    gjorOppTilsagn = boolean("gjor_opp_tilsagn"),
    periode = periode("periode"),
    lopenummer = int("lopenummer"),
    status = DelutbetalingStatus.valueOf(string("status")),
    fakturaStatusSistOppdatert = localDateTimeOrNull("faktura_status_sist_oppdatert"),
    faktura = Delutbetaling.Faktura(
        fakturanummer = string("fakturanummer"),
        status = stringOrNull("faktura_status")?.let { FakturaStatusType.valueOf(it) },
    ),
)
