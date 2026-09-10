package no.nav.mulighetsrommet.api.brukerutbetaling.db

import arrow.core.NonEmptySet
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.helved.HelVedStatus
import no.nav.mulighetsrommet.api.contracts.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class BrukerUtbetalingDbo(
    val id: UUID,
    val sakId: String,
    val behandlingId: Int,
    val belop: Int,
    val transaksjonsDato: LocalDate,
    val tilskuddstype: HelVedUtbetaling.Tilskuddstype,
    val tiltakskode: HelVedUtbetaling.Tiltakskode,
    val saksbehandler: NavIdent,
    val beslutter: NavIdent,
    val besluttetTidspunkt: Instant,
    val helVedStatus: HelVedStatus.Status?,
    val helVedStatusError: HelVedStatus.StatusError?,
    val kostnadssted: Kostnadssted,
    val tilskuddVedtakId: UUID,
) {
    data class Kostnadssted(
        val navn: String,
        val enhetsnummer: NavEnhetNummer,
    )
}

data class UpsertBrukerUtbetalingDbo(
    val id: UUID,
    val sakId: String,
    val belop: Int,
    val transaksjonsDato: LocalDate,
    val tilskuddstype: HelVedUtbetaling.Tilskuddstype,
    val tiltakskode: HelVedUtbetaling.Tiltakskode,
    val saksbehandler: NavIdent,
    val beslutter: NavIdent,
    val besluttetTidspunkt: Instant,
    val tilskuddVedtakId: UUID,
)

class BrukerUtbetalingQueries(private val session: Session) {
    fun insert(utbetaling: UpsertBrukerUtbetalingDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into bruker_utbetaling (
                id,
                sak_id,
                behandling_id,
                belop,
                tilskuddstype,
                tiltakskode,
                saksbehandler,
                beslutter,
                besluttet_tidspunkt,
                transaksjon_dato,
                tilskudd_vedtak_id
            ) select
                :id::uuid,
                :sak_id,
                tv.lopenummer,
                :belop,
                :tilskuddstype,
                :tiltakskode,
                :saksbehandler,
                :beslutter,
                :besluttet_tidspunkt,
                :transaksjon_dato,
                tv.id
            from tilskudd_vedtak tv
            where tv.id = :tilskudd_vedtak_id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to utbetaling.id,
            "sak_id" to utbetaling.sakId,
            "belop" to utbetaling.belop,
            "tilskuddstype" to utbetaling.tilskuddstype.name,
            "tiltakskode" to utbetaling.tiltakskode.name,
            "saksbehandler" to utbetaling.saksbehandler.value,
            "beslutter" to utbetaling.beslutter.value,
            "besluttet_tidspunkt" to utbetaling.besluttetTidspunkt,
            "transaksjon_dato" to utbetaling.transaksjonsDato,
            "tilskudd_vedtak_id" to utbetaling.tilskuddVedtakId,
        )

        session.execute(queryOf(query, params))
    }

    fun getByTilskuddVedtak(tilskuddVedtakId: UUID): BrukerUtbetalingDbo? {
        @Language("PostgreSQL")
        val query = """
            select
                bruker_utbetaling.*,
                nav_enhet.enhetsnummer as kostnadssted_enhetsnummer,
                nav_enhet.navn as kostnadssted_navn
            from bruker_utbetaling
                inner join tilskudd_vedtak on bruker_utbetaling.tilskudd_vedtak_id = tilskudd_vedtak.id
                inner join nav_enhet on nav_enhet.enhetsnummer = tilskudd_vedtak.kostnadssted
            where tilskudd_vedtak.id = :id::uuid
        """.trimIndent()

        return session.single(
            queryOf(query, mapOf("id" to tilskuddVedtakId)),
        ) {
            it.toBrukerUtbetalingDbo()
        }
    }

    fun getLastFromTilskudd(tilskuddId: UUID): BrukerUtbetalingDbo? {
        @Language("PostgreSQL")
        val query = """
            select
                bruker_utbetaling.*,
                nav_enhet.enhetsnummer as kostnadssted_enhetsnummer,
                nav_enhet.navn as kostnadssted_navn
            from bruker_utbetaling
                inner join tilskudd_vedtak on bruker_utbetaling.tilskudd_vedtak_id = tilskudd_vedtak.id
                inner join nav_enhet on nav_enhet.enhetsnummer = tilskudd_vedtak.kostnadssted
            where tilskudd_vedtak.tilskudd_id = :tilskudd_id::uuid
            order by bruker_utbetaling.behandling_id desc
            limit 1
        """.trimIndent()

        return session.single(
            queryOf(query, mapOf("tilskudd_id" to tilskuddId)),
        ) {
            it.toBrukerUtbetalingDbo()
        }
    }

    fun setHelVedStatus(id: UUID, behandlingIds: NonEmptySet<Int>, status: HelVedStatus) {
        @Language("PostgreSQL")
        val query = """
            update bruker_utbetaling set
                hel_ved_status = :status,
                hel_ved_status_error = :status_error::jsonb
            where
                id = :id::uuid
                and behandling_id = any(:behandling_ids::int[])
        """.trimIndent()

        session.execute(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "behandling_ids" to behandlingIds.toIntArray(),
                    "status" to status.status.name,
                    "status_error" to Json.encodeToString(status.error),
                ),
            ),
        )
    }
}

private fun Row.toBrukerUtbetalingDbo() = BrukerUtbetalingDbo(
    id = uuid("id"),
    sakId = string("sak_id"),
    behandlingId = int("behandling_id"),
    belop = int("belop"),
    transaksjonsDato = localDate("transaksjon_dato"),
    tilskuddstype = HelVedUtbetaling.Tilskuddstype.valueOf(string("tilskuddstype")),
    tiltakskode = HelVedUtbetaling.Tiltakskode.valueOf(string("tiltakskode")),
    saksbehandler = NavIdent(string("saksbehandler")),
    beslutter = NavIdent(string("beslutter")),
    besluttetTidspunkt = instant("besluttet_tidspunkt"),
    helVedStatus = stringOrNull("hel_ved_status")?.let { HelVedStatus.Status.valueOf(it) },
    helVedStatusError = stringOrNull("hel_ved_status_error")?.let { Json.decodeFromString(it) },
    kostnadssted = BrukerUtbetalingDbo.Kostnadssted(
        navn = string("kostnadssted_navn"),
        enhetsnummer = NavEnhetNummer(string("kostnadssted_enhetsnummer")),
    ),
    tilskuddVedtakId = uuid("tilskudd_vedtak_id"),
)
