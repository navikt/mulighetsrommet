package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultatDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.samletVedtakResultatStatusTag
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.UUID

class TilskuddBehandlingQueries(private val session: Session) {
    fun upsert(dbo: TilskuddBehandling): Unit = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tilskudd_behandling (
                id,
                gjennomforing_id,
                soknad_journalpost_id,
                status
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :soknad_journalpost_id,
                :status
            ) on conflict (id) do update set
                gjennomforing_id = excluded.gjennomforing_id,
                soknad_journalpost_id = excluded.soknad_journalpost_id,
                status = excluded.status
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "soknad_journalpost_id" to dbo.soknadJournalpostId,
            "status" to dbo.status.name,
        )

        execute(queryOf(query, params))

        dbo.tilskudd.forEachIndexed { index, tilskudd ->
            upsertTilskudd(behandling = dbo, tilskudd = tilskudd, lopenummer = index + 1)
        }
    }

    private fun upsertTilskudd(
        behandling: TilskuddBehandling,
        tilskudd: TilskuddDbo,
        lopenummer: Int,
    ): Unit = withTransaction(session) {
        @Language("PostgreSQL")
        val tilskuddQuery = """
            insert into tilskudd (
                id,
                tilskudd_behandling_id,
                tilskudd_opplaering_id
            ) values (
                :id::uuid,
                :tilskudd_behandling_id::uuid,
                (select id from tilskudd_opplaering where kode = :tilskudd_opplaering_kode)
            ) on conflict (id) do update set
                tilskudd_behandling_id = excluded.tilskudd_behandling_id,
                tilskudd_opplaering_id = excluded.tilskudd_opplaering_id
        """.trimIndent()

        val tilskuddParams = mapOf(
            "id" to tilskudd.id,
            "tilskudd_behandling_id" to behandling.id,
            "tilskudd_opplaering_kode" to tilskudd.tilskuddOpplaeringType.name,
        )

        execute(queryOf(tilskuddQuery, tilskuddParams))

        @Language("PostgreSQL")
        val vedtakQuery = """
            insert into tilskudd_vedtak (
                id,
                tilskudd_id,
                tilskudd_behandling_id,
                lopenummer,
                periode,
                kostnadssted,
                soknad_dato,
                soknad_belop,
                soknad_valuta,
                vedtak_resultat,
                kommentar_vedtaksbrev,
                kommentar_intern,
                utbetaling_mottaker,
                kid,
                belop,
                valuta
            ) values (
                coalesce(
                    (
                        select tv.id
                        from tilskudd_vedtak tv
                        where tv.tilskudd_id = :tilskudd_id::uuid
                        order by tv.lopenummer asc
                        limit 1
                    ),
                    :id::uuid
                ),
                :tilskudd_id::uuid,
                :tilskudd_behandling_id::uuid,
                :lopenummer,
                :periode::daterange,
                :kostnadssted,
                :soknad_dato,
                :soknad_belop,
                :soknad_valuta::currency,
                :vedtak_resultat,
                :kommentar_vedtaksbrev,
                :kommentar_intern,
                :utbetaling_mottaker,
                :kid,
                :belop,
                :valuta::currency
            ) on conflict (id) do update set
                tilskudd_id = excluded.tilskudd_id,
                tilskudd_behandling_id = excluded.tilskudd_behandling_id,
                lopenummer = excluded.lopenummer,
                periode = excluded.periode,
                kostnadssted = excluded.kostnadssted,
                soknad_dato = excluded.soknad_dato,
                soknad_belop = excluded.soknad_belop,
                soknad_valuta = excluded.soknad_valuta,
                vedtak_resultat = excluded.vedtak_resultat,
                kommentar_vedtaksbrev = excluded.kommentar_vedtaksbrev,
                kommentar_intern = excluded.kommentar_intern,
                utbetaling_mottaker = excluded.utbetaling_mottaker,
                kid = excluded.kid,
                belop = excluded.belop,
                valuta = excluded.valuta
        """.trimIndent()

        val vedtakParams = mapOf(
            "id" to tilskudd.id,
            "tilskudd_id" to tilskudd.id,
            "tilskudd_behandling_id" to behandling.id,
            "lopenummer" to lopenummer,
            "periode" to behandling.periode.toDaterange(),
            "kostnadssted" to behandling.kostnadssted.value,
            "soknad_dato" to behandling.soknadDato,
            "soknad_belop" to tilskudd.soknadBelop.belop,
            "soknad_valuta" to tilskudd.soknadBelop.valuta.name,
            "vedtak_resultat" to tilskudd.vedtakResultat.name,
            "kommentar_vedtaksbrev" to tilskudd.kommentarVedtaksbrev,
            "kommentar_intern" to behandling.kommentarIntern,
            "utbetaling_mottaker" to tilskudd.utbetalingMottaker.name,
            "kid" to tilskudd.kid?.value,
            "belop" to tilskudd.utbetalingBelop?.belop,
            "valuta" to tilskudd.utbetalingBelop?.valuta?.name,
        )

        execute(queryOf(vedtakQuery, vedtakParams))
    }

    fun setJournalpostId(tilskuddBehandlingId: UUID, journalpostId: String) {
        @Language("PostgreSQL")
        val query = """
            update tilskudd_vedtak
              set vedtak_journalpost_id = :journalpost_id,
              vedtak_journalfort_tidspunkt = now()
            where
              tilskudd_behandling_id = :behandling_id::uuid
        """.trimIndent()

        val params = mapOf(
            "behandling_id" to tilskuddBehandlingId,
            "journalpost_id" to journalpostId,
        )
        session.execute(queryOf(query, params))
    }

    fun setJournalpostDistribueringId(tilskuddBehandlingId: UUID, journalpostDistribueringId: String) {
        @Language("PostgreSQL")
        val query = """
            update tilskudd_vedtak
              set vedtak_journalpost_distribuering_id = :journalpost_distribuering_id,
              vedtak_distribuert_tidspunkt = now()
            where
              tilskudd_behandling_id = :behandling_id::uuid
        """.trimIndent()

        val params = mapOf(
            "behandling_id" to tilskuddBehandlingId,
            "journalpost_distribuering_id" to journalpostDistribueringId,
        )
        session.execute(queryOf(query, params))
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

    fun setUtbetaling(tilskuddId: UUID, utbetalingId: UUID) {
        @Language("PostgreSQL")
        val query = """
            update tilskudd_vedtak
            set utbetaling_id = :utbetaling_id::uuid
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to tilskuddId, "utbetaling_id" to utbetalingId)))
    }

    fun setBrukerUtbetaling(tilskuddId: UUID, brukerUtbetalingId: UUID) {
        @Language("PostgreSQL")
        val query = """
            update tilskudd_vedtak
            set bruker_utbetaling_id = bruker_utbetaling.id ,
                bruker_utbetaling_behandling_id = bruker_utbetaling.behandling_id
            from bruker_utbetaling
            where tilskudd_vedtak.id = :id::uuid and bruker_utbetaling.id = :bruker_utbetaling_id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to tilskuddId, "bruker_utbetaling_id" to brukerUtbetalingId)))
    }

    fun get(id: UUID): TilskuddBehandlingDto? {
        @Language("PostgreSQL")
        val query = """
            select * from view_tilskudd_behandling
            where id = :id::uuid
        """.trimIndent()

        return session.single(queryOf(query, mapOf("id" to id))) { it.toTilskuddBehandlingDto() }
    }

    fun getVedtakJournalpostDistribueringId(behandlingId: UUID): String? {
        @Language("PostgreSQL")
        val query = """
            select vedtak_journalpost_distribuering_id
            from tilskudd_vedtak
            where tilskudd_behandling_id = :behandling_id::uuid
        """.trimIndent()

        return session.single(queryOf(query, mapOf("behandling_id" to behandlingId))) { it.stringOrNull("vedtak_journalpost_distribuering_id") }
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

        return session.list(
            queryOf(
                query,
                mapOf("gjennomforing_id" to gjennomforingId),
            ),
        ) { it.toTilskuddBehandlingDto() }
    }
}

private val viewJson = Json {
    ignoreUnknownKeys = true
}

private data class TilskuddBehandlingViewRow(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: String,
    @SerialName("gjennomforing_id")
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    @SerialName("soknad_journalpost_id")
    val soknadJournalpostId: String,
    @SerialName("vedtak_json")
    val vedtakJson: String,
)

@Serializable
private data class TilskuddVedtakViewRow(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @SerialName("soknad_dato")
    @Serializable(with = LocalDateSerializer::class)
    val soknadDato: LocalDate,
    val periode: String,
    @SerialName("kostnadssted_enhetsnummer")
    val kostnadsstedEnhetsnummer: String,
    @SerialName("kostnadssted_navn")
    val kostnadsstedNavn: String,
    val tilskuddOpplaeringType: Opplaeringtilskudd.Kode,
    val soknadBelop: ValutaBelop,
    val utbetalingBelop: ValutaBelop?,
    val vedtakResultat: VedtakResultatViewRow,
    val kommentarVedtaksbrev: String?,
    val utbetalingMottaker: TilskuddMottaker,
    val kid: Kid?,
    val kommentarIntern: String?,
    val vedtakJournalpostId: String?,
)

@Serializable
private data class VedtakResultatViewRow(
    val type: no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat,
)

private fun Row.toTilskuddBehandlingViewRow(): TilskuddBehandlingViewRow {
    return TilskuddBehandlingViewRow(
        id = uuid("id"),
        status = string("status"),
        gjennomforingId = uuid("gjennomforing_id"),
        soknadJournalpostId = string("soknad_journalpost_id"),
        vedtakJson = string("vedtak_json"),
    )
}

private fun Row.toTilskuddBehandlingDto(): TilskuddBehandlingDto {
    return toTilskuddBehandlingViewRow().toDto()
}

private fun TilskuddBehandlingViewRow.toDto(): TilskuddBehandlingDto {
    val vedtak = viewJson.decodeFromString<List<TilskuddVedtakViewRow>>(vedtakJson)
    require(vedtak.isNotEmpty()) { "Tilskuddsbehandling med id $id mangler vedtak" }

    val tilskudd = vedtak.map { it.toDto() }
    val firstVedtak = vedtak.first()

    return TilskuddBehandlingDto(
        id = id,
        gjennomforingId = gjennomforingId,
        soknadJournalpostId = soknadJournalpostId,
        soknadDato = firstVedtak.soknadDato,
        periode = firstVedtak.periode.toPeriode(),
        kostnadssted = KostnadsstedDto(
            navn = firstVedtak.kostnadsstedNavn,
            enhetsnummer = NavEnhetNummer(firstVedtak.kostnadsstedEnhetsnummer),
        ),
        tilskudd = tilskudd,
        status = TilskuddBehandlingStatusDto(TilskuddBehandlingStatus.valueOf(status)),
        kommentarIntern = firstVedtak.kommentarIntern,
        vedtakJournalpostId = firstVedtak.vedtakJournalpostId,
        samletVedtakResultat = samletVedtakResultatStatusTag(tilskudd.map { it.vedtakResultat.type }),
    )
}

private fun TilskuddVedtakViewRow.toDto(): TilskuddOpplaeringDto {
    return TilskuddOpplaeringDto(
        id = id,
        tilskuddOpplaeringType = tilskuddOpplaeringType,
        soknadBelop = soknadBelop,
        vedtakResultat = VedtakResultatDto(vedtakResultat.type),
        kommentarVedtaksbrev = kommentarVedtaksbrev,
        utbetalingMottaker = utbetalingMottaker,
        kid = kid,
        utbetalingBelop = utbetalingBelop,
    )
}

private fun String.toPeriode(): Periode {
    val (start, end) = removeSurrounding("[", ")").split(",")
    return Periode(
        start = LocalDate.parse(start.trim()),
        slutt = LocalDate.parse(end.trim()),
    )
}
