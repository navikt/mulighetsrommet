package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.utbetaling.BesluttDelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.utils.periode
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.*

class DelutbetalingQueries(private val session: Session) {
    fun upsert(delutbetaling: DelutbetalingDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into delutbetaling (
                tilsagn_id,
                utbetaling_id,
                belop,
                periode,
                lopenummer,
                fakturanummer,
                opprettet_av
            ) values (
                :tilsagn_id::uuid,
                :utbetaling_id::uuid,
                :belop,
                daterange(:periode_start, :periode_slutt),
                :lopenummer,
                :fakturanummer,
                :opprettet_av
            ) on conflict (utbetaling_id, tilsagn_id) do update set
                belop                = excluded.belop,
                opprettet_av         = excluded.opprettet_av,
                besluttet_av         = null,
                besluttet_tidspunkt  = null,
                besluttelse          = null,
                aarsaker             = null,
                forklaring           = null,
                periode              = delutbetaling.periode,
                lopenummer           = delutbetaling.lopenummer,
                fakturanummer        = delutbetaling.fakturanummer;
        """.trimIndent()

        val params = mapOf(
            "tilsagn_id" to delutbetaling.tilsagnId,
            "utbetaling_id" to delutbetaling.utbetalingId,
            "belop" to delutbetaling.belop,
            "periode_start" to delutbetaling.periode.start,
            "periode_slutt" to delutbetaling.periode.slutt,
            "lopenummer" to delutbetaling.lopenummer,
            "fakturanummer" to delutbetaling.fakturanummer,
            "opprettet_av" to delutbetaling.opprettetAv.value,
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

    fun getByUtbetalingId(id: UUID): List<DelutbetalingDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select
                tilsagn_id,
                utbetaling_id,
                belop,
                periode,
                lopenummer,
                fakturanummer,
                opprettet_av,
                created_at,
                besluttet_av,
                besluttet_tidspunkt,
                besluttelse,
                aarsaker,
                forklaring
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
                tilsagn_id,
                utbetaling_id,
                belop,
                periode,
                lopenummer,
                fakturanummer,
                opprettet_av,
                created_at,
                besluttet_av,
                besluttet_tidspunkt,
                besluttelse,
                aarsaker,
                forklaring
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

    fun godkjenn(
        utbetalingId: UUID,
        tilsagnId: UUID,
        navIdent: NavIdent,
    ) {
        @Language("PostgreSQL")
        val query = """
            update delutbetaling set
                besluttet_av = :nav_ident,
                besluttet_tidspunkt = now(),
                besluttelse = 'GODKJENT'
            where utbetaling_id = :utbetaling_id::uuid
            and tilsagn_id = :tilsagn_id::uuid
        """.trimIndent()

        val params = mapOf(
            "utbetaling_id" to utbetalingId,
            "tilsagn_id" to tilsagnId,
            "nav_ident" to navIdent.value,
        )

        session.execute(queryOf(query, params))
    }

    fun avvis(
        utbetalingId: UUID,
        navIdent: NavIdent,
        request: BesluttDelutbetalingRequest.AvvistDelutbetalingRequest,
    ) {
        @Language("PostgreSQL")
        val query = """
            update delutbetaling set
                besluttet_av = :nav_ident,
                besluttet_tidspunkt = now(),
                besluttelse = :besluttelse::besluttelse,
                aarsaker = :aarsaker,
                forklaring = :forklaring
            where utbetaling_id = :utbetaling_id::uuid
            and tilsagn_id = :tilsagn_id::uuid
        """.trimIndent()

        val params = mapOf(
            "utbetaling_id" to utbetalingId,
            "tilsagn_id" to request.tilsagnId,
            "nav_ident" to navIdent.value,
            "besluttelse" to Besluttelse.AVVIST.name,
            "aarsaker" to session.createTextArray(request.aarsaker),
            "forklaring" to request.forklaring,
        )

        session.execute(queryOf(query, params))
    }

    data class DelutbetalingOppgaveData(
        val delutbetaling: DelutbetalingDto,
        val gjennomforingId: UUID,
        val tiltakskode: Tiltakskode,
        val gjennomforingsnavn: String
    )

    fun getOppgaveData(
        kostnadssteder: List<String>?,
        tiltakskoder: List<Tiltakskode>?,
    ): List<DelutbetalingOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            select
                delutbetaling.tilsagn_id,
                delutbetaling.utbetaling_id,
                delutbetaling.belop,
                delutbetaling.periode,
                delutbetaling.lopenummer,
                delutbetaling.fakturanummer,
                delutbetaling.opprettet_av,
                delutbetaling.created_at,
                delutbetaling.besluttet_av,
                delutbetaling.besluttet_tidspunkt,
                delutbetaling.besluttelse,
                delutbetaling.aarsaker,
                delutbetaling.forklaring,
                tilsagn.gjennomforing_id,
                gjennomforing.navn,
                tiltakstype.tiltakskode
            from delutbetaling
                inner join tilsagn on tilsagn.id = delutbetaling.tilsagn_id
                inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
                inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
            where
                (besluttelse is null or besluttelse = 'AVVIST') and
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
}

private fun Row.toDelutbetalingDto(): DelutbetalingDto {
    val besluttetAv = stringOrNull("besluttet_av")?.let { NavIdent(it) }
    val besluttelse = stringOrNull("besluttelse")?.let { Besluttelse.valueOf(it) }
    val besluttetTidspunkt = localDateTimeOrNull("besluttet_tidspunkt")
    val aarsaker = arrayOrNull<String>("aarsaker")?.toList() ?: emptyList()
    val forklaring = stringOrNull("forklaring")

    return when (besluttelse) {
        null -> DelutbetalingDto.DelutbetalingTilGodkjenning(
            tilsagnId = uuid("tilsagn_id"),
            utbetalingId = uuid("utbetaling_id"),
            opprettetAv = NavIdent(string("opprettet_av")),
            opprettetTidspunkt = localDateTime("created_at"),
            belop = int("belop"),
            periode = periode("periode"),
            lopenummer = int("lopenummer"),
            fakturanummer = string("fakturanummer"),
        )
        Besluttelse.GODKJENT -> {
            requireNotNull(besluttetTidspunkt)
            requireNotNull(besluttetAv)
            DelutbetalingDto.DelutbetalingOverfortTilUtbetaling(
                tilsagnId = uuid("tilsagn_id"),
                utbetalingId = uuid("utbetaling_id"),
                belop = int("belop"),
                periode = periode("periode"),
                opprettetAv = NavIdent(string("opprettet_av")),
                opprettetTidspunkt = localDateTime("created_at"),
                besluttetAv = besluttetAv,
                besluttetTidspunkt = besluttetTidspunkt,
                lopenummer = int("lopenummer"),
                fakturanummer = string("fakturanummer"),
            )
        }
        Besluttelse.AVVIST -> {
            requireNotNull(besluttetTidspunkt)
            requireNotNull(besluttetAv)
            DelutbetalingDto.DelutbetalingAvvist(
                tilsagnId = uuid("tilsagn_id"),
                utbetalingId = uuid("utbetaling_id"),
                belop = int("belop"),
                periode = periode("periode"),
                opprettetAv = NavIdent(string("opprettet_av")),
                opprettetTidspunkt = localDateTime("created_at"),
                besluttetAv = besluttetAv,
                besluttetTidspunkt = besluttetTidspunkt,
                lopenummer = int("lopenummer"),
                fakturanummer = string("fakturanummer"),
                aarsaker = aarsaker,
                forklaring = forklaring,
            )
        }
    }
}
