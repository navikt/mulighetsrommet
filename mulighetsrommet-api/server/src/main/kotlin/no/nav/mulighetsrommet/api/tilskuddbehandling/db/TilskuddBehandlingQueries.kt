package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultatDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.samletVedtakResultatStatusTag
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.datatypes.toPeriode
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import org.intellij.lang.annotations.Language
import java.util.UUID
import kotlin.collections.map

class TilskuddBehandlingQueries(private val session: Session) {
    fun upsert(dbo: TilskuddBehandling): Unit = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tilskudd_behandling (
                id,
                gjennomforing_id,
                status,
                type,
                behandlende_enhet
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :status,
                :type,
                :behandlende_enhet
            ) on conflict (id) do update set
                gjennomforing_id = excluded.gjennomforing_id,
                status = excluded.status,
                type = excluded.type,
                behandlende_enhet = excluded.behandlende_enhet
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "status" to dbo.status.name,
            "type" to dbo.type.name,
            "behandlende_enhet" to dbo.behandlendeEnhet.toString(),
        )

        execute(queryOf(query, params))

        val (tiltaksnummer, sisteTilskuddLopenummer) = getTiltaksnummerBase(dbo.gjennomforingId)
        dbo.tilskudd.forEachIndexed { index, tilskuddVedtak ->
            val nesteLopenummer = sisteTilskuddLopenummer + index + 1
            val nyttTilskuddsnummer = Tilskuddsnummer.from(tiltaksnummer, nesteLopenummer)
            upsertTilskudd(behandling = dbo, tilskuddVedtak = tilskuddVedtak, nyttTilskuddsnummer = nyttTilskuddsnummer)
        }
    }

    private fun getTiltaksnummerBase(gjennomforingId: UUID): Pair<Tiltaksnummer, Int> = withTransaction(session) {
        @Language("PostgreSQL")
        val tilskuddsnummerQuery = """
            select
                g.lopenummer,
                (
                    select coalesce(max(split_part(t.tilskuddsnummer, '-', 2)::int), 0)
                    from tilskudd t
                    where t.gjennomforing_id = g.id
                ) as siste_tilskudd_lopenummer
            from gjennomforing g
            where g.id = ?::uuid
        """.trimIndent()

        val nyTilskuddsnummer = single(queryOf(tilskuddsnummerQuery, gjennomforingId)) { row ->
            val tiltaksnummer = Tiltaksnummer(row.string("lopenummer"))
            val sisteTilskuddLopenummer = row.int("siste_tilskudd_lopenummer")
            Pair(tiltaksnummer, sisteTilskuddLopenummer)
        }
        requireNotNull(nyTilskuddsnummer) { "Mangler gjennomforing med id $gjennomforingId" }
        nyTilskuddsnummer
    }

    private fun upsertTilskudd(
        behandling: TilskuddBehandling,
        tilskuddVedtak: TilskuddVedtak,
        nyttTilskuddsnummer: Tilskuddsnummer,
    ): Unit = withTransaction(session) {
        @Language("PostgreSQL")
        val tilskuddQuery = """
            insert into tilskudd (
                id,
                tilskudd_opplaering_id,
                tilskuddsnummer,
                gjennomforing_id
            ) values (
                :id::uuid,
                (select id from tilskudd_opplaering where kode = :tilskudd_opplaering_kode),
                :tilskuddsnummer,
                :gjennomforing_id::uuid
            ) on conflict (id) do update set
                tilskudd_opplaering_id = excluded.tilskudd_opplaering_id
        """.trimIndent()

        val tilskuddParams = mapOf(
            "id" to tilskuddVedtak.tilskuddId,
            "tilskudd_opplaering_kode" to tilskuddVedtak.tilskuddOpplaeringType.name,
            "tilskuddsnummer" to nyttTilskuddsnummer.value,
            "gjennomforing_id" to behandling.gjennomforingId,
        )

        execute(queryOf(tilskuddQuery, tilskuddParams))

        @Language("PostgreSQL")
        val vedtakQuery = """
            with neste_lopenummer as (
                     select coalesce(max(tv.lopenummer), 0) + 1 as nytt_lopenummer
                     from tilskudd_vedtak tv
                     where tv.tilskudd_id = :tilskudd_id::uuid
                 ),
            update as (insert into tilskudd_vedtak (
                id,
                tilskudd_id,
                tilskudd_behandling_id,
                lopenummer,
                periode,
                kostnadssted,
                soknad_journalpost_id,
                soknad_dato,
                soknad_belop,
                soknad_valuta,
                vedtak_resultat,
                kommentar_vedtaksbrev,
                kommentar_intern,
                utbetaling_mottaker,
                kid,
                bruker_utbetaling_type,
                belop,
                valuta
            ) values (
             :id::uuid,
             :tilskudd_id::uuid,
             :tilskudd_behandling_id::uuid,
             (select nytt_lopenummer from neste_lopenummer),
             :periode::daterange,
             :kostnadssted,
             :soknad_journalpost_id,
             :soknad_dato,
             :soknad_belop,
             :soknad_valuta::currency,
             :vedtak_resultat,
             :kommentar_vedtaksbrev,
             :kommentar_intern,
             :utbetaling_mottaker,
             :kid,
             :bruker_utbetaling_type,
             :belop,
             :valuta::currency
         ) on conflict (id) do update set
              tilskudd_id = excluded.tilskudd_id,
              tilskudd_behandling_id = excluded.tilskudd_behandling_id,
              periode = excluded.periode,
              kostnadssted = excluded.kostnadssted,
              soknad_journalpost_id = excluded.soknad_journalpost_id,
              soknad_dato = excluded.soknad_dato,
              soknad_belop = excluded.soknad_belop,
              soknad_valuta = excluded.soknad_valuta,
              vedtak_resultat = excluded.vedtak_resultat,
              kommentar_vedtaksbrev = excluded.kommentar_vedtaksbrev,
              kommentar_intern = excluded.kommentar_intern,
              utbetaling_mottaker = excluded.utbetaling_mottaker,
              kid = excluded.kid,
              bruker_utbetaling_type = excluded.bruker_utbetaling_type,
              belop = excluded.belop,
              valuta = excluded.valuta
         returning id)
         select id from update
        """.trimIndent()

        val vedtakParams = mapOf(
            "id" to tilskuddVedtak.id,
            "tilskudd_id" to tilskuddVedtak.tilskuddId,
            "tilskudd_behandling_id" to behandling.id,
            "periode" to tilskuddVedtak.periode.toDaterange(),
            "kostnadssted" to tilskuddVedtak.kostnadssted.value,
            "soknad_journalpost_id" to tilskuddVedtak.soknadJournalpostId.value,
            "soknad_dato" to tilskuddVedtak.soknadDato,
            "soknad_belop" to tilskuddVedtak.soknadBelop.belop,
            "soknad_valuta" to tilskuddVedtak.soknadBelop.valuta.name,
            "vedtak_resultat" to tilskuddVedtak.vedtakResultat.name,
            "kommentar_vedtaksbrev" to tilskuddVedtak.kommentarVedtaksbrev,
            "kommentar_intern" to tilskuddVedtak.kommentarIntern,
            "utbetaling_mottaker" to tilskuddVedtak.utbetalingMottaker.name,
            "kid" to tilskuddVedtak.kid?.value,
            "bruker_utbetaling_type" to tilskuddVedtak.brukerUtbetalingType?.name,
            "belop" to tilskuddVedtak.utbetalingBelop?.belop,
            "valuta" to tilskuddVedtak.utbetalingBelop?.valuta?.name,
        )

        execute(queryOf(vedtakQuery, vedtakParams))
    }

    fun acquireLockTilskudd(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            select tilskudd_id from tilskudd_vedtak where tilskudd_id = ?::uuid for update
        """.trimIndent()
        session.execute(queryOf(query, id))
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

    fun setUtbetalingTilskuddVedtak(id: UUID, utbetalingId: UUID) {
        @Language("PostgreSQL")
        val query = """
            update tilskudd_vedtak
            set utbetaling_id = :utbetaling_id::uuid
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id, "utbetaling_id" to utbetalingId)))
    }

    fun get(id: UUID): TilskuddBehandlingDto? {
        @Language("PostgreSQL")
        val query = """
            select id, status, type, gjennomforing_id, behandlende_enhet
            from tilskudd_behandling
            where id = :id::uuid
        """.trimIndent()

        return withTransaction(session) {
            val behandling = single(queryOf(query, mapOf("id" to id))) { it.toTilskuddBehandlingDbo() }
                ?: return@withTransaction null
            val vedtak = getVedtakForBehandlingIds(listOf(id))
            behandling.toDto(vedtak.map { it.dto })
        }
    }

    private fun Session.getVedtakForBehandlingIds(behandlingIds: Collection<UUID>): List<TilskuddVedtakRow> {
        if (behandlingIds.isEmpty()) return emptyList()

        @Language("PostgreSQL")
        val query = """
        select *
        from view_tilskudd_vedtak v
        where v.tilskudd_behandling_id = any(:behandling_ids::uuid[])
        order by v.lopenummer desc
        """.trimIndent()

        val behandlingIdsArray = createUuidArray(behandlingIds.toList())
        return list(queryOf(query, mapOf("behandling_ids" to behandlingIdsArray))) { row ->
            TilskuddVedtakRow(
                behandlingId = row.uuid("tilskudd_behandling_id"),
                dto = row.toTilskuddOpplaeringDto(),
            )
        }
    }

    fun getVedtakJournalpostDistribueringId(behandlingId: UUID): String? {
        @Language("PostgreSQL")
        val query = """
            select vedtak_journalpost_distribuering_id
            from tilskudd_vedtak
            where tilskudd_behandling_id = :behandling_id::uuid
        """.trimIndent()

        return session.single(
            queryOf(
                query,
                mapOf("behandling_id" to behandlingId),
            ),
        ) { it.stringOrNull("vedtak_journalpost_distribuering_id") }
    }

    fun getVedtakJournalpostId(behandlingId: UUID): String? {
        @Language("PostgreSQL")
        val query = """
            select vedtak_journalpost_id
            from tilskudd_vedtak
            where tilskudd_behandling_id = :behandling_id::uuid
        """.trimIndent()

        return session.single(
            queryOf(
                query,
                mapOf("behandling_id" to behandlingId),
            ),
        ) { it.stringOrNull("vedtak_journalpost_id") }
    }

    fun getOrError(id: UUID): TilskuddBehandlingDto {
        return checkNotNull(get(id)) { "Tilskuddsbehadling med id $id finnes ikke" }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilskuddBehandlingDto> {
        @Language("PostgreSQL")
        val query = """
            select id, status, type, gjennomforing_id, behandlende_enhet
            from tilskudd_behandling
            where gjennomforing_id = :gjennomforing_id::uuid
        """.trimIndent()

        return withTransaction(session) {
            val behandlinger = list(
                queryOf(
                    query,
                    mapOf("gjennomforing_id" to gjennomforingId),
                ),
            ) { it.toTilskuddBehandlingDbo() }
            if (behandlinger.isEmpty()) {
                return@withTransaction emptyList()
            }

            val vedtakByBehandlingId = getVedtakForBehandlingIds(behandlinger.map { it.id })
                .groupBy { it.behandlingId }

            behandlinger.map { behandling ->
                behandling.toDto(vedtakByBehandlingId[behandling.id].orEmpty().map { it.dto })
            }
        }
    }
}

private data class TilskuddVedtakRow(
    val behandlingId: UUID,
    val dto: TilskuddOpplaeringDto,
)

private fun Row.toTilskuddBehandlingDbo(): TilskuddBehandlingDbo {
    return TilskuddBehandlingDbo(
        id = uuid("id"),
        status = TilskuddBehandlingStatus.valueOf(string("status")),
        type = TilskuddBehandlingType.valueOf(string("type")),
        gjennomforingId = uuid("gjennomforing_id"),
        behandlendeEnhet = stringOrNull("behandlende_enhet")?.let { NavEnhetNummer(it) },
    )
}

private fun Row.toTilskuddOpplaeringDto(): TilskuddOpplaeringDto {
    val soknadBelop = ValutaBelop(
        belop = int("soknad_belop"),
        valuta = Valuta.valueOf(string("soknad_valuta")),
    )
    val utbetalingBelop = intOrNull("belop")?.let { belop ->
        ValutaBelop(
            belop = belop,
            valuta = Valuta.valueOf(string("valuta")),
        )
    }

    return TilskuddOpplaeringDto(
        id = uuid("id"),
        tilskuddId = uuid("tilskudd_id"),
        tilskuddsnummer = Tilskuddsnummer(string("tilskuddsnummer")),
        tilskuddOpplaeringType = Opplaeringtilskudd.Kode.valueOf(string("tilskudd_opplaering_type")),
        soknadDato = localDate("soknad_dato"),
        soknadJournalpostId = JournalpostId(string("soknad_journalpost_id")),
        soknadBelop = soknadBelop,
        periode = string("periode").toPeriode(),
        kostnadssted = KostnadsstedDto(
            navn = string("kostnadssted_navn"),
            enhetsnummer = NavEnhetNummer(string("kostnadssted")),
        ),
        vedtakResultat = VedtakResultatDto(VedtakResultat.valueOf(string("vedtak_resultat"))),
        kommentarVedtaksbrev = stringOrNull("kommentar_vedtaksbrev"),
        utbetalingMottaker = TilskuddMottaker.valueOf(string("utbetaling_mottaker")),
        kid = stringOrNull("kid")?.let(Kid::parse),
        brukerUtbetalingType = stringOrNull("bruker_utbetaling_type")?.let(BrukerUtbetalingType::valueOf),
        utbetalingBelop = utbetalingBelop,
        kommentarIntern = stringOrNull("kommentar_intern"),
        vedtakJournalpostId = stringOrNull("vedtak_journalpost_id"),
    )
}

private fun TilskuddBehandlingDbo.toDto(tilskudd: List<TilskuddOpplaeringDto>): TilskuddBehandlingDto {
    require(tilskudd.isNotEmpty()) { "Tilskuddsbehandling med id $id mangler vedtak" }

    return TilskuddBehandlingDto(
        id = id,
        gjennomforingId = gjennomforingId,
        tilskudd = tilskudd,
        status = TilskuddBehandlingStatusDto(status),
        type = type,
        samletVedtakResultat = samletVedtakResultatStatusTag(tilskudd.map { it.vedtakResultat.type }),
        behandlendeEnhet = behandlendeEnhet,
    )
}
