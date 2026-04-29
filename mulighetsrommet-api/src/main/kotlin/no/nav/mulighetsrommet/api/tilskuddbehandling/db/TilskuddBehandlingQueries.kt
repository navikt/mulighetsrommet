package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringDto
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavEnhetNummer
import org.intellij.lang.annotations.Language
import java.util.UUID

class TilskuddBehandlingQueries(private val session: Session) {
    fun upsert(dbo: TilskuddBehandlingDbo): Unit = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tilskudd_behandling (
                id,
                gjennomforing_id,
                soknad_journalpost_id,
                soknad_dato,
                periode,
                kostnadssted,
                status
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :soknad_journalpost_id,
                :soknad_dato,
                :periode::daterange,
                :kostnadssted,
                :status
            ) on conflict (id) do update set
                gjennomforing_id = excluded.gjennomforing_id,
                soknad_journalpost_id = excluded.soknad_journalpost_id,
                soknad_dato = excluded.soknad_dato,
                periode = excluded.periode,
                kostnadssted = excluded.kostnadssted,
                status = excluded.status
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "soknad_journalpost_id" to dbo.soknadJournalpostId,
            "soknad_dato" to dbo.soknadDato,
            "periode" to dbo.periode.toDaterange(),
            "kostnadssted" to dbo.kostnadssted.value,
            "status" to dbo.status.name,
        )

        execute(queryOf(query, params))

        dbo.vedtak.forEach { upsertVedtak(dbo.id, it) }
    }

    private fun upsertVedtak(tilskuddsbehandlingId: UUID, vedtak: TilskuddVedtakDbo): Unit = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tilskudd_vedtak (
                id,
                tilskudd_behandling_id,
                tilskudd_opplaering_id,
                soknad_belop,
                soknad_valuta,
                vedtak_resultat,
                kommentar_vedtaksbrev,
                utbetaling_mottaker
            ) values (
                :id::uuid,
                :tilskudd_behandling_id::uuid,
                (select id from tilskudd_opplaering where kode = :tilskudd_opplaering_kode),
                :soknad_belop,
                :soknad_valuta::currency,
                :vedtak_resultat,
                :kommentar_vedtaksbrev,
                :utbetaling_mottaker
            ) on conflict (id) do update set
                tilskudd_behandling_id = excluded.tilskudd_behandling_id,
                tilskudd_opplaering_id = excluded.tilskudd_opplaering_id,
                soknad_belop = excluded.soknad_belop,
                soknad_valuta = excluded.soknad_valuta,
                vedtak_resultat = excluded.vedtak_resultat,
                kommentar_vedtaksbrev = excluded.kommentar_vedtaksbrev,
                utbetaling_mottaker = excluded.utbetaling_mottaker
        """.trimIndent()

        val params = mapOf(
            "id" to vedtak.id,
            "tilskudd_behandling_id" to tilskuddsbehandlingId,
            "tilskudd_opplaering_kode" to vedtak.tilskuddOpplaeringType.name,
            "soknad_belop" to vedtak.soknadBelop,
            "soknad_valuta" to vedtak.soknadValuta.name,
            "vedtak_resultat" to vedtak.vedtakResultat.name,
            "kommentar_vedtaksbrev" to vedtak.kommentarVedtaksbrev,
            "utbetaling_mottaker" to vedtak.utbetalingMottaker,
        )

        execute(queryOf(query, params))
    }

    fun setStatus(id: UUID, status: TilskuddBehandlingStatus) {
        @Language("PostgreSQL")
        val query = """
            update tilskudd_behandling
            set status = :status
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id, "status" to status.name)))
    }

    fun get(id: UUID): TilskuddBehandlingDto? {
        @Language("PostgreSQL")
        val query = """
            select * from view_tilskudd_behandling
            where id = :id::uuid
        """.trimIndent()

        return session.single(queryOf(query, mapOf("id" to id))) { it.toTilskuddBehandlingDto() }
    }

    fun getOrError(id: UUID): TilskuddBehandlingDto {
        return checkNotNull(get(id)) { "Tilskuddsbehadling med id $id finnes ikke" }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilskuddBehandlingDto> {
        @Language("PostgreSQL")
        val query = """
            select * from view_tilskudd_behandling
            where gjennomforing_id = :gjennomforing_id::uuid
        """.trimIndent()

        return session.list(queryOf(query, mapOf("gjennomforing_id" to gjennomforingId))) { it.toTilskuddBehandlingDto() }
    }
}

private fun Row.toTilskuddBehandlingDto() = TilskuddBehandlingDto(
    id = uuid("id"),
    gjennomforingId = uuid("gjennomforing_id"),
    soknadJournalpostId = string("soknad_journalpost_id"),
    soknadDato = localDate("soknad_dato"),
    periode = periode("periode"),
    kostnadssted = NavEnhetNummer(string("kostnadssted")),
    tilskudd = Json.decodeFromString<List<TilskuddOpplaeringDto>>(string("vedtak_json")),
    status = TilskuddBehandlingStatusDto(TilskuddBehandlingStatus.valueOf(string("status"))),
)
