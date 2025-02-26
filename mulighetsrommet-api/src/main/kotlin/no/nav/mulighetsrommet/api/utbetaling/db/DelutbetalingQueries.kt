package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.utils.periode
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class DelutbetalingQueries(private val session: Session) {
    fun upsert(delutbetaling: DelutbetalingDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into delutbetaling (
                id,
                tilsagn_id,
                utbetaling_id,
                belop,
                periode,
                lopenummer,
                fakturanummer
            ) values (
                :id::uuid,
                :tilsagn_id::uuid,
                :utbetaling_id::uuid,
                :belop,
                daterange(:periode_start, :periode_slutt),
                :lopenummer,
                :fakturanummer
            ) on conflict (utbetaling_id, tilsagn_id) do update set
                belop                = excluded.belop,
                periode              = delutbetaling.periode,
                lopenummer           = delutbetaling.lopenummer,
                fakturanummer        = delutbetaling.fakturanummer;
        """.trimIndent()

        val params = mapOf(
            "id" to delutbetaling.id,
            "tilsagn_id" to delutbetaling.tilsagnId,
            "utbetaling_id" to delutbetaling.utbetalingId,
            "belop" to delutbetaling.belop,
            "periode_start" to delutbetaling.periode.start,
            "periode_slutt" to delutbetaling.periode.slutt,
            "lopenummer" to delutbetaling.lopenummer,
            "fakturanummer" to delutbetaling.fakturanummer,
        )

        execute(queryOf(query, params))
        TotrinnskontrollQueries(this).behandler(
            entityId = delutbetaling.id,
            navIdent = delutbetaling.opprettetAv,
            type = TotrinnskontrollType.OPPRETT,
            aarsaker = null,
            forklaring = null,
            tidspunkt = LocalDateTime.now(),
        )
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
                delutbetaling.id,
                tilsagn_id,
                utbetaling_id,
                belop,
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

    fun getByUtbetalingId(id: UUID): List<DelutbetalingDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                tilsagn_id,
                utbetaling_id,
                belop,
                periode,
                lopenummer,
                fakturanummer
            from delutbetaling
            where utbetaling_id = ?
            order by created_at desc
        """.trimIndent()

        return list(queryOf(query, id)) { it.toDelutbetalingDto() }
    }

    fun get(id: UUID): DelutbetalingDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                tilsagn_id,
                utbetaling_id,
                belop,
                periode,
                lopenummer,
                fakturanummer
            from delutbetaling
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to id)

        return single(queryOf(query, params)) { it.toDelutbetalingDto() }
    }

    private fun getIdOrThrow(utbetalingId: UUID, tilsagnId: UUID): UUID {
        // Fetch the primary_id from primary_table using (utbetaling_id, tilsagn_id)
        @Language("PostgreSQL")
        val query = """
            select id from delutbetaling
            where utbetaling_id = :utbetaling_id and tilsagn_id = :tilsagn_id
        """.trimIndent()

        val params = mapOf(
            "utbetaling_id" to utbetalingId,
            "tilsagn_id" to tilsagnId,
        )

        val id = session.run(queryOf(query, params).map { it.uuid("id") }.asSingle)
        requireNotNull(id) {
            "No matching primary_id found for utbetalingId=$utbetalingId, tilsagnId=$tilsagnId"
        }
        return id
    }

    data class DelutbetalingOppgaveData(
        val delutbetaling: DelutbetalingDto,
        val gjennomforingId: UUID,
        val tiltakskode: Tiltakskode,
        val gjennomforingsnavn: String,
    )

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
                delutbetaling.belop,
                delutbetaling.periode,
                delutbetaling.lopenummer,
                delutbetaling.fakturanummer,
                tilsagn.gjennomforing_id,
                gjennomforing.navn,
                tiltakstype.tiltakskode
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
                tiltakskode = Tiltakskode.valueOf(it.string("tiltakskode")),
            )
        }
    }

    private fun Row.toDelutbetalingDto(): DelutbetalingDto {
        val id = uuid("id")
        val opprettelse = TotrinnskontrollQueries(session).get(entityId = id, type = TotrinnskontrollType.OPPRETT)
        requireNotNull(opprettelse)

        return when (opprettelse) {
            is Totrinnskontroll.Ubesluttet -> DelutbetalingDto.DelutbetalingTilGodkjenning(
                id = uuid("id"),
                tilsagnId = uuid("tilsagn_id"),
                utbetalingId = uuid("utbetaling_id"),
                opprettelse = opprettelse,
                belop = int("belop"),
                periode = periode("periode"),
                lopenummer = int("lopenummer"),
                fakturanummer = string("fakturanummer"),
            )
            is Totrinnskontroll.Besluttet -> when (opprettelse.besluttelse) {
                Besluttelse.GODKJENT -> {
                    DelutbetalingDto.DelutbetalingOverfortTilUtbetaling(
                        id = uuid("id"),
                        tilsagnId = uuid("tilsagn_id"),
                        utbetalingId = uuid("utbetaling_id"),
                        belop = int("belop"),
                        periode = periode("periode"),
                        opprettelse = opprettelse,
                        lopenummer = int("lopenummer"),
                        fakturanummer = string("fakturanummer"),
                    )
                }
                Besluttelse.AVVIST -> {
                    DelutbetalingDto.DelutbetalingAvvist(
                        id = uuid("id"),
                        tilsagnId = uuid("tilsagn_id"),
                        utbetalingId = uuid("utbetaling_id"),
                        belop = int("belop"),
                        periode = periode("periode"),
                        opprettelse = opprettelse,
                        lopenummer = int("lopenummer"),
                        fakturanummer = string("fakturanummer"),
                    )
                }
            }
        }
    }
}
