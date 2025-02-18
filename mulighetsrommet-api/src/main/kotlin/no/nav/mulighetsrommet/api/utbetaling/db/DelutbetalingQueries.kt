package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.db.ToTrinnskontrollQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.db.ToTrinnskontrollType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.ToTrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.BesluttDelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.utils.periode
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.*

class DelutbetalingQueries(private val session: Session) {
    fun upsert(delutbetaling: DelutbetalingDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into delutbetaling (
                tilsagn_id,
                utbetaling_id,
                belop,
                periode,
                lopenummer,
                fakturanummer
            ) values (
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
                fakturanummer        = delutbetaling.fakturanummer
            RETURNING id;
        """.trimIndent()

        val params = mapOf(
            "tilsagn_id" to delutbetaling.tilsagnId,
            "utbetaling_id" to delutbetaling.utbetalingId,
            "belop" to delutbetaling.belop,
            "periode_start" to delutbetaling.periode.start,
            "periode_slutt" to delutbetaling.periode.slutt,
            "lopenummer" to delutbetaling.lopenummer,
            "fakturanummer" to delutbetaling.fakturanummer,
        )

        val id = run(queryOf(query, params).map { it.uuid("id") }.asSingle)
        requireNotNull(id)

        ToTrinnskontrollQueries(this).opprett(
            entityId = id,
            opprettetAv = delutbetaling.opprettetAv,
            aarsaker = emptyList(),
            forklaring = null,
            type = ToTrinnskontrollType.OPPRETT,
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

    fun get(utbetalingId: UUID, tilsagnId: UUID): DelutbetalingDto? = with(session) {
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
            where utbetaling_id = :utbetaling_id
            and tilsagn_id = :tilsagn_id
        """.trimIndent()

        val params = mapOf(
            "utbetaling_id" to utbetalingId,
            "tilsagn_id" to tilsagnId,
        )

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

    fun godkjenn(
        utbetalingId: UUID,
        tilsagnId: UUID,
        navIdent: NavIdent,
    ) = withTransaction(session) {
        val id = getIdOrThrow(utbetalingId, tilsagnId)

        ToTrinnskontrollQueries(session).beslutt(
            entityId = id,
            besluttetAv = navIdent,
            besluttelse = Besluttelse.GODKJENT,
            aarsaker = null,
            forklaring = null,
            type = ToTrinnskontrollType.OPPRETT,
        )
    }

    fun avvis(
        utbetalingId: UUID,
        navIdent: NavIdent,
        request: BesluttDelutbetalingRequest.AvvistDelutbetalingRequest,
    ) {
        val id = getIdOrThrow(utbetalingId, request.tilsagnId)

        ToTrinnskontrollQueries(session).beslutt(
            entityId = id,
            besluttetAv = navIdent,
            besluttelse = Besluttelse.AVVIST,
            aarsaker = request.aarsaker,
            forklaring = request.forklaring,
            type = ToTrinnskontrollType.OPPRETT,
        )
    }

    data class DelutbetalingOppgaveData(
        val delutbetaling: DelutbetalingDto,
        val gjennomforingId: UUID,
        val tiltakskode: Tiltakskode,
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
                tiltakstype.tiltakskode
            from delutbetaling
                inner join to_trinnskontroll on to_trinnskontroll.entity_id = delutbetaling.id
                inner join tilsagn on tilsagn.id = delutbetaling.tilsagn_id
                inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
                inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
            where
                (to_trinnskontroll.besluttelse is null or to_trinnskontroll.besluttelse = 'AVVIST') and
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
                tiltakskode = Tiltakskode.valueOf(it.string("tiltakskode")),
            )
        }
    }

    private fun Row.toDelutbetalingDto(): DelutbetalingDto {
        val id = uuid("id")
        val opprettelse = ToTrinnskontrollQueries(session).get(entityId = id, type = ToTrinnskontrollType.OPPRETT)
        requireNotNull(opprettelse)

        return when (opprettelse) {
            is ToTrinnskontroll.Ubesluttet -> DelutbetalingDto.DelutbetalingTilGodkjenning(
                tilsagnId = uuid("tilsagn_id"),
                utbetalingId = uuid("utbetaling_id"),
                opprettetAv = opprettelse.opprettetAv,
                opprettetTidspunkt = opprettelse.opprettetTidspunkt,
                belop = int("belop"),
                periode = periode("periode"),
                lopenummer = int("lopenummer"),
                fakturanummer = string("fakturanummer"),
            )
            is ToTrinnskontroll.Besluttet -> when (opprettelse.besluttelse) {
                Besluttelse.GODKJENT -> {
                    DelutbetalingDto.DelutbetalingOverfortTilUtbetaling(
                        tilsagnId = uuid("tilsagn_id"),
                        utbetalingId = uuid("utbetaling_id"),
                        belop = int("belop"),
                        periode = periode("periode"),
                        opprettetAv = opprettelse.opprettetAv,
                        opprettetTidspunkt = opprettelse.opprettetTidspunkt,
                        besluttetAv = opprettelse.besluttetAv,
                        besluttetTidspunkt = opprettelse.besluttetTidspunkt,
                        lopenummer = int("lopenummer"),
                        fakturanummer = string("fakturanummer"),
                    )
                }
                Besluttelse.AVVIST -> {
                    DelutbetalingDto.DelutbetalingAvvist(
                        tilsagnId = uuid("tilsagn_id"),
                        utbetalingId = uuid("utbetaling_id"),
                        belop = int("belop"),
                        periode = periode("periode"),
                        opprettetAv = opprettelse.opprettetAv,
                        opprettetTidspunkt = opprettelse.opprettetTidspunkt,
                        besluttetAv = opprettelse.besluttetAv,
                        besluttetTidspunkt = opprettelse.besluttetTidspunkt,
                        lopenummer = int("lopenummer"),
                        fakturanummer = string("fakturanummer"),
                        aarsaker = opprettelse.aarsaker,
                        forklaring = opprettelse.forklaring,
                    )
                }
            }
        }
    }
}
