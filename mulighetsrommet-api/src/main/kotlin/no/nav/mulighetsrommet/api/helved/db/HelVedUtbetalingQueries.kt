package no.nav.mulighetsrommet.api.helved.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.helved.HelVedStatus
import no.nav.mulighetsrommet.api.clients.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class HelVedUtbetalingDbo(
    val id: UUID,
    val sakId: String,
    val behandlingId: String,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val belop: Int,
    val tilskuddstype: HelVedUtbetaling.Tilskuddstype,
    val tiltakskode: HelVedUtbetaling.Tiltakskode,
    val saksbehandler: NavIdent,
    val beslutter: NavIdent,
    val besluttetTidspunkt: Instant,
    val helVedStatus: HelVedStatus.Status?,
    val helVedStatusError: HelVedStatus.StatusError?,
)

class HelVedUtbetalingQueries(private val session: Session) {
    fun insert(utbetaling: HelVedUtbetaling) {
        @Language("PostgreSQL")
        val query = """
            insert into hel_ved_utbetaling (
                id,
                sak_id,
                behandling_id,
                periode_fom,
                periode_tom,
                belop,
                tilskuddstype,
                tiltakskode,
                saksbehandler,
                beslutter,
                besluttet_tidspunkt
            ) values (
                :id::uuid,
                :sak_id,
                :behandling_id,
                :periode_fom,
                :periode_tom,
                :belop,
                :tilskuddstype,
                :tiltakskode,
                :saksbehandler,
                :beslutter,
                :besluttet_tidspunkt
            )
        """.trimIndent()

        val params = mapOf(
            "id" to utbetaling.id,
            "sak_id" to utbetaling.sakId,
            "behandling_id" to utbetaling.behandlingId,
            "periode_fom" to utbetaling.periode.fom,
            "periode_tom" to utbetaling.periode.tom,
            "belop" to utbetaling.belop,
            "tilskuddstype" to utbetaling.tilskuddstype.name,
            "tiltakskode" to utbetaling.tiltakskode.name,
            "saksbehandler" to utbetaling.saksbehandler.value,
            "beslutter" to utbetaling.beslutter.value,
            "besluttet_tidspunkt" to utbetaling.besluttetTidspunkt,
        )

        session.execute(queryOf(query, params))
    }

    fun getByTilskudd(tilskuddId: UUID): HelVedUtbetalingDbo? {
        @Language("PostgreSQL")
        val query = """
            select hel_ved_utbetaling.* from hel_ved_utbetaling
            inner join tilskudd on tilskudd.hel_ved_utbetaling_id = hel_ved_utbetaling.id
            where tilskudd.id = :id::uuid
        """.trimIndent()

        return session.single(queryOf(query, mapOf("id" to tilskuddId))) { it.toHelVedUtbetalingDbo() }
    }

    fun setHelVedStatus(id: UUID, status: HelVedStatus) {
        @Language("PostgreSQL")
        val query = """
            update hel_ved_utbetaling set
                hel_ved_status = :status,
                hel_ved_status_error = :status_error::jsonb
            where id = :id::uuid
        """.trimIndent()

        session.execute(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "status" to status.status.name,
                    "status_error" to Json.encodeToString(status.error),
                ),
            ),
        )
    }
}

private fun Row.toHelVedUtbetalingDbo() = HelVedUtbetalingDbo(
    id = uuid("id"),
    sakId = string("sak_id"),
    behandlingId = string("behandling_id"),
    periodeFom = localDate("periode_fom"),
    periodeTom = localDate("periode_tom"),
    belop = int("belop"),
    tilskuddstype = HelVedUtbetaling.Tilskuddstype.valueOf(string("tilskuddstype")),
    tiltakskode = HelVedUtbetaling.Tiltakskode.valueOf(string("tiltakskode")),
    saksbehandler = NavIdent(string("saksbehandler")),
    beslutter = NavIdent(string("beslutter")),
    besluttetTidspunkt = instant("besluttet_tidspunkt"),
    helVedStatus = stringOrNull("hel_ved_status")?.let { HelVedStatus.Status.valueOf(it) },
    helVedStatusError = stringOrNull("hel_ved_status_error")?.let { Json.decodeFromString(it) },
)
