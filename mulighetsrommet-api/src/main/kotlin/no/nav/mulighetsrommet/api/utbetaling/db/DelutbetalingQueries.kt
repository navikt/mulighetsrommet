package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.utils.periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.oppgaver.OppgaveTiltakstype
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
                frigjor_tilsagn,
                periode,
                lopenummer,
                fakturanummer
            ) values (
                :id::uuid,
                :tilsagn_id::uuid,
                :utbetaling_id::uuid,
                :status::delutbetaling_status,
                :belop,
                :frigjor_tilsagn::boolean,
                :periode::daterange,
                :lopenummer,
                :fakturanummer
            ) on conflict (utbetaling_id, tilsagn_id) do update set
                status               = excluded.status,
                belop                = excluded.belop,
                frigjor_tilsagn      = excluded.frigjor_tilsagn,
                periode              = delutbetaling.periode,
                lopenummer           = delutbetaling.lopenummer,
                fakturanummer        = delutbetaling.fakturanummer;
        """.trimIndent()

        val params = mapOf(
            "id" to delutbetaling.id,
            "tilsagn_id" to delutbetaling.tilsagnId,
            "utbetaling_id" to delutbetaling.utbetalingId,
            "status" to delutbetaling.status.name,
            "belop" to delutbetaling.belop,
            "frigjor_tilsagn" to delutbetaling.frigjorTilsagn,
            "periode" to delutbetaling.periode.toDaterange(),
            "lopenummer" to delutbetaling.lopenummer,
            "fakturanummer" to delutbetaling.fakturanummer,
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

    fun getSkalSendesTilOkonomi(tilsagnId: UUID): List<DelutbetalingDto> {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                tilsagn_id,
                utbetaling_id,
                status,
                belop,
                frigjor_tilsagn,
                periode,
                lopenummer,
                fakturanummer
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

    fun getByUtbetalingId(id: UUID): List<DelutbetalingDto> {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                tilsagn_id,
                utbetaling_id,
                status,
                belop,
                frigjor_tilsagn,
                periode,
                lopenummer,
                fakturanummer
            from delutbetaling
            where utbetaling_id = ?
            order by created_at desc
        """.trimIndent()

        return session.list(queryOf(query, id)) { it.toDelutbetalingDto() }
    }

    fun get(id: UUID): DelutbetalingDto? {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                tilsagn_id,
                utbetaling_id,
                status,
                belop,
                frigjor_tilsagn,
                periode,
                lopenummer,
                fakturanummer
            from delutbetaling
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toDelutbetalingDto() }
    }

    fun getOppgaveData(
        kostnadssteder: List<String>?,
        tiltakskoder: List<Tiltakskode>?,
    ): List<DelutbetalingOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            select
                delutbetaling.id,
                delutbetaling.tilsagn_id,
                delutbetaling.utbetaling_id,
                delutbetaling.status,
                delutbetaling.belop,
                delutbetaling.frigjor_tilsagn,
                delutbetaling.periode,
                delutbetaling.lopenummer,
                delutbetaling.fakturanummer,
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
            "tiltakskoder" to tiltakskoder?.let { session.createArrayOf("tiltakskode", it) },
            "kostnadssteder" to kostnadssteder?.let { session.createTextArray(it) },
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
}

private fun Row.toDelutbetalingDto() = DelutbetalingDto(
    id = uuid("id"),
    tilsagnId = uuid("tilsagn_id"),
    utbetalingId = uuid("utbetaling_id"),
    belop = int("belop"),
    frigjorTilsagn = boolean("frigjor_tilsagn"),
    periode = periode("periode"),
    lopenummer = int("lopenummer"),
    fakturanummer = string("fakturanummer"),
    status = DelutbetalingStatus.valueOf(string("status")),
)
