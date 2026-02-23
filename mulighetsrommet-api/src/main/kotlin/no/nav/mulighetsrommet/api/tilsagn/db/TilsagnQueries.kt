package no.nav.mulighetsrommet.api.tilsagn.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri.Input
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri.Output
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.BestillingStatusType
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.util.UUID

class TilsagnQueries(private val session: Session) {
    fun upsert(dbo: TilsagnDbo): Unit = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tilsagn (
                id,
                gjennomforing_id,
                periode,
                lopenummer,
                bestillingsnummer,
                bestilling_status,
                kostnadssted,
                status,
                type,
                valuta,
                belop_brukt,
                belop_beregnet,
                beregning_type,
                beregning_sats,
                beregning_antall_plasser,
                beregning_antall_timer_oppfolging_per_deltaker,
                beregning_prisbetingelser,
                kommentar,
                beskrivelse,
                datastream_periode_start,
                datastream_periode_slutt
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :periode::daterange,
                :lopenummer,
                :bestillingsnummer,
                :bestilling_status,
                :kostnadssted,
                :status::tilsagn_status,
                :type::tilsagn_type,
                :valuta::currency,
                :belop_brukt,
                :belop_beregnet,
                :beregning_type::tilsagn_beregning_type,
                :beregning_sats,
                :beregning_antall_plasser,
                :beregning_antall_timer_oppfolging_per_deltaker,
                :beregning_prisbetingelser,
                :kommentar,
                :beskrivelse,
                :datastream_periode_start,
                :datastream_periode_slutt
            )
            on conflict (id) do update set
                gjennomforing_id                        = excluded.gjennomforing_id,
                periode                                 = excluded.periode,
                lopenummer                              = excluded.lopenummer,
                bestillingsnummer                       = excluded.bestillingsnummer,
                bestilling_status                       = excluded.bestilling_status,
                kostnadssted                            = excluded.kostnadssted,
                status                                  = excluded.status,
                type                                    = excluded.type,
                valuta                                  = excluded.valuta,
                belop_brukt                             = excluded.belop_brukt,
                belop_beregnet                          = excluded.belop_beregnet,
                beregning_type                          = excluded.beregning_type,
                beregning_sats                                 = excluded.beregning_sats,
                beregning_antall_plasser                       = excluded.beregning_antall_plasser,
                beregning_antall_timer_oppfolging_per_deltaker = excluded.beregning_antall_timer_oppfolging_per_deltaker,
                beregning_prisbetingelser                      = excluded.beregning_prisbetingelser,
                kommentar                               = excluded.kommentar,
                beskrivelse                             = excluded.beskrivelse,
                datastream_periode_start                = excluded.datastream_periode_start,
                datastream_periode_slutt                = excluded.datastream_periode_slutt
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "periode" to dbo.periode.toDaterange(),
            "lopenummer" to dbo.lopenummer,
            "status" to TilsagnStatus.TIL_GODKJENNING.name,
            "bestillingsnummer" to dbo.bestillingsnummer,
            "bestilling_status" to dbo.bestillingStatus?.name,
            "kostnadssted" to dbo.kostnadssted.value,
            "type" to dbo.type.name,
            "belop_brukt" to dbo.belopBrukt.belop,
            "belop_beregnet" to dbo.beregning.output.pris.belop,
            "valuta" to dbo.belopBrukt.valuta.name,
            "beregning_type" to when (dbo.beregning) {
                is TilsagnBeregningFri -> TilsagnBeregningType.FRI
                is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED
                is TilsagnBeregningPrisPerManedsverk -> TilsagnBeregningType.PRIS_PER_MANEDSVERK
                is TilsagnBeregningPrisPerUkesverk -> TilsagnBeregningType.PRIS_PER_UKESVERK
                is TilsagnBeregningPrisPerHeleUkesverk -> TilsagnBeregningType.PRIS_PER_HELE_UKESVERK
                is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING
            }.name,
            "datastream_periode_start" to dbo.periode.start,
            "datastream_periode_slutt" to dbo.periode.getLastInclusiveDate(),
            "kommentar" to dbo.kommentar,
            "beskrivelse" to dbo.beskrivelse,
        )
        val beregningParams = when (dbo.beregning) {
            is TilsagnBeregningFri -> mapOf(
                "beregning_prisbetingelser" to dbo.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> mapOf(
                "beregning_sats" to dbo.beregning.input.sats.belop,
                "beregning_antall_plasser" to dbo.beregning.input.antallPlasser,
            )

            is TilsagnBeregningPrisPerManedsverk -> mapOf(
                "beregning_sats" to dbo.beregning.input.sats.belop,
                "beregning_antall_plasser" to dbo.beregning.input.antallPlasser,
                "beregning_prisbetingelser" to dbo.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningPrisPerUkesverk -> mapOf(
                "beregning_sats" to dbo.beregning.input.sats.belop,
                "beregning_antall_plasser" to dbo.beregning.input.antallPlasser,
                "beregning_prisbetingelser" to dbo.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningPrisPerHeleUkesverk -> mapOf(
                "beregning_sats" to dbo.beregning.input.sats.belop,
                "beregning_antall_plasser" to dbo.beregning.input.antallPlasser,
                "beregning_prisbetingelser" to dbo.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> mapOf(
                "beregning_sats" to dbo.beregning.input.sats.belop,
                "beregning_antall_plasser" to dbo.beregning.input.antallPlasser,
                "beregning_antall_timer_oppfolging_per_deltaker" to dbo.beregning.input.antallTimerOppfolgingPerDeltaker,
                "beregning_prisbetingelser" to dbo.beregning.input.prisbetingelser,
            )
        }

        execute(queryOf(query, params + beregningParams))

        if (dbo.beregning is TilsagnBeregningFri) {
            upsertTilsagnBeregningFriLinjer(dbo.id, dbo.beregning.input.linjer)
        }
    }

    private fun TransactionalSession.upsertTilsagnBeregningFriLinjer(
        tilsagnId: UUID,
        linjer: List<TilsagnBeregningFri.InputLinje>,
    ) {
        @Language("PostgreSQL")
        val deleteExistingQuery = """
            delete from tilsagn_fri_beregning
            where tilsagn_id = ?
        """.trimIndent()
        execute(queryOf(deleteExistingQuery, tilsagnId))

        @Language("PostgreSQL")
        val query = """
            insert into tilsagn_fri_beregning (
                    id,
                    tilsagn_id,
                    beskrivelse,
                    valuta,
                    belop,
                    antall
                ) values (
                    :id::uuid,
                    :tilsagn_id::uuid,
                    :beskrivelse,
                    :valuta::currency,
                    :belop,
                    :antall
                )
        """.trimIndent()
        val paramCollection = linjer.map {
            mapOf(
                "id" to it.id,
                "tilsagn_id" to tilsagnId,
                "beskrivelse" to it.beskrivelse,
                "valuta" to it.pris.valuta.name,
                "belop" to it.pris.belop,
                "antall" to it.antall,
            )
        }

        batchPreparedNamedStatement(query, paramCollection)
    }

    fun setBruktBelop(id: UUID, belop: ValutaBelop) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                belop_brukt = :belop
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "belop" to belop.belop,
        )

        session.execute(queryOf(query, params))
    }

    fun getNextLopenummeByGjennomforing(gjennomforingId: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            select coalesce(max(lopenummer), 0) + 1 as lopenummer
            from tilsagn
            where gjennomforing_id = ?
        """.trimIndent()

        return session.requireSingle(queryOf(query, gjennomforingId)) { it.int("lopenummer") }
    }

    fun getOrError(id: UUID): Tilsagn {
        return checkNotNull(get(id)) { "Tilsagn med id $id finnes ikke" }
    }

    fun get(id: UUID): Tilsagn? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_tilsagn
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toTilsagn() }
    }

    fun getOrError(bestillingsnummer: String): Tilsagn {
        return checkNotNull(get(bestillingsnummer)) { "Tilsagn med bestillingsnummer $bestillingsnummer finnes ikke" }
    }

    fun get(bestillingsnummer: String): Tilsagn? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_tilsagn
            where bestillingsnummer = ?
        """.trimIndent()

        return session.single(queryOf(query, bestillingsnummer)) { it.toTilsagn() }
    }

    fun getAll(
        typer: List<TilsagnType>? = null,
        gjennomforingId: UUID? = null,
        arrangorer: Set<Organisasjonsnummer>? = null,
        statuser: List<TilsagnStatus>? = null,
        periodeIntersectsWith: Periode? = null,
        valuta: Valuta? = null,
    ): List<Tilsagn> {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_tilsagn
            where
              (:typer::tilsagn_type[] is null or type = any(:typer::tilsagn_type[]))
              and (:gjennomforing_id::uuid is null or gjennomforing_id = :gjennomforing_id::uuid)
              and (:arrangorer::text[] is null or arrangor_organisasjonsnummer = any(:arrangorer))
              and (:statuser::tilsagn_status[] is null or status::tilsagn_status = any(:statuser))
              and (:periode::daterange is null or periode && :periode::daterange)
              and (:valuta::currency is null or valuta = :valuta::currency)
            order by created_at desc
        """.trimIndent()

        val params = mapOf(
            "typer" to typer?.let { session.createArrayOfTilsagnType(it) },
            "gjennomforing_id" to gjennomforingId,
            "arrangorer" to arrangorer?.let { list -> session.createArrayOfValue(list) { it.value } },
            "statuser" to statuser?.let { session.createArrayOfTilsagnStatus(it) },
            "periode" to periodeIntersectsWith?.toDaterange(),
            "valuta" to valuta?.name,
        )

        return session.list(queryOf(query, params)) { it.toTilsagn() }
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from tilsagn where id = ?
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    fun setStatus(id: UUID, status: TilsagnStatus) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set status = :status::tilsagn_status where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id, "status" to status.name)))
    }

    fun setBestillingStatus(bestillingsnummer: String, status: BestillingStatusType) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn
            set bestilling_status = ?
             where bestillingsnummer = ?
        """.trimIndent()

        session.execute(queryOf(query, status.name, bestillingsnummer))
    }

    fun setJournalpostId(id: UUID, journalpostId: String) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn
              set journalpost_id = :journalpost_id
            where
              id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "journalpost_id" to journalpostId,
        )
        session.execute(queryOf(query, params))
    }

    fun setJournalpostDistribueringId(id: UUID, journalpostDistribueringId: String) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn
              set journalpost_distribuering_id = :journalpost_distribuering_id
            where
              id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "journalpost_distribuering_id" to journalpostDistribueringId,
        )
        session.execute(queryOf(query, params))
    }

    private fun Row.toTilsagn(): Tilsagn {
        val id = uuid("id")
        val valuta = string("valuta").let { Valuta.valueOf(it) }

        val beregning = getBeregning(id, valuta, TilsagnBeregningType.valueOf(string("beregning_type")))

        return Tilsagn(
            id = uuid("id"),
            type = TilsagnType.valueOf(string("type")),
            tiltakstype = Tilsagn.Tiltakstype(
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
                navn = string("tiltakstype_navn"),
            ),
            gjennomforing = Tilsagn.Gjennomforing(
                id = uuid("gjennomforing_id"),
                lopenummer = Tiltaksnummer(string("gjennomforing_lopenummer")),
                navn = string("gjennomforing_navn"),
            ),
            belopBrukt = int("belop_brukt").withValuta(valuta),
            periode = periode("periode"),
            lopenummer = int("lopenummer"),
            bestilling = Tilsagn.Bestilling(
                bestillingsnummer = string("bestillingsnummer"),
                status = stringOrNull("bestilling_status")?.let { BestillingStatusType.valueOf(it) },
            ),
            kostnadssted = NavEnhetDbo(
                enhetsnummer = NavEnhetNummer(string("kostnadssted")),
                navn = string("kostnadssted_navn"),
                type = Norg2Type.valueOf(string("kostnadssted_type")),
                overordnetEnhet = stringOrNull("kostnadssted_overordnet_enhet")?.let { NavEnhetNummer(it) },
                status = NavEnhetStatus.valueOf(string("kostnadssted_status")),
            ),
            arrangor = Tilsagn.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
            ),
            beregning = beregning,
            status = TilsagnStatus.valueOf(string("status")),
            kommentar = stringOrNull("kommentar"),
            beskrivelse = stringOrNull("beskrivelse"),
            journalpost = stringOrNull("journalpost_id")?.let { journalpostId ->
                Tilsagn.Journalpost(
                    id = journalpostId,
                    distribueringId = stringOrNull("journalpost_distribuering_id"),
                )
            },
        )
    }

    private fun Row.getBeregning(id: UUID, valuta: Valuta, beregning: TilsagnBeregningType): TilsagnBeregning {
        return when (beregning) {
            TilsagnBeregningType.FRI -> {
                TilsagnBeregningFri(
                    input = Input(
                        linjer = getTilsagnBeregningFriLinjer(id),
                        prisbetingelser = stringOrNull("beregning_prisbetingelser"),
                    ),
                    output = Output(
                        pris = int("belop_beregnet").withValuta(valuta),
                    ),
                )
            }

            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED -> TilsagnBeregningFastSatsPerTiltaksplassPerManed(
                input = TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                    periode = periode("periode"),
                    sats = int("beregning_sats").withValuta(valuta),
                    antallPlasser = int("beregning_antall_plasser"),
                ),
                output = TilsagnBeregningFastSatsPerTiltaksplassPerManed.Output(
                    pris = int("belop_beregnet").withValuta(valuta),
                ),
            )

            TilsagnBeregningType.PRIS_PER_MANEDSVERK -> TilsagnBeregningPrisPerManedsverk(
                input = TilsagnBeregningPrisPerManedsverk.Input(
                    periode = periode("periode"),
                    sats = int("beregning_sats").withValuta(valuta),
                    antallPlasser = int("beregning_antall_plasser"),
                    prisbetingelser = stringOrNull("beregning_prisbetingelser"),
                ),
                output = TilsagnBeregningPrisPerManedsverk.Output(
                    pris = int("belop_beregnet").withValuta(valuta),
                ),
            )

            TilsagnBeregningType.PRIS_PER_UKESVERK -> TilsagnBeregningPrisPerUkesverk(
                input = TilsagnBeregningPrisPerUkesverk.Input(
                    periode = periode("periode"),
                    sats = ValutaBelop(int("beregning_sats"), valuta),
                    antallPlasser = int("beregning_antall_plasser"),
                    prisbetingelser = stringOrNull("beregning_prisbetingelser"),
                ),
                output = TilsagnBeregningPrisPerUkesverk.Output(
                    pris = int("belop_beregnet").withValuta(valuta),
                ),
            )

            TilsagnBeregningType.PRIS_PER_HELE_UKESVERK -> TilsagnBeregningPrisPerHeleUkesverk(
                input = TilsagnBeregningPrisPerHeleUkesverk.Input(
                    periode = periode("periode"),
                    sats = int("beregning_sats").withValuta(valuta),
                    antallPlasser = int("beregning_antall_plasser"),
                    prisbetingelser = stringOrNull("beregning_prisbetingelser"),
                ),
                output = TilsagnBeregningPrisPerHeleUkesverk.Output(
                    pris = int("belop_beregnet").withValuta(valuta),
                ),
            )

            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING -> TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker(
                input = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
                    periode = periode("periode"),
                    sats = int("beregning_sats").withValuta(valuta),
                    antallPlasser = int("beregning_antall_plasser"),
                    antallTimerOppfolgingPerDeltaker = int("beregning_antall_timer_oppfolging_per_deltaker"),
                    prisbetingelser = stringOrNull("beregning_prisbetingelser"),
                ),
                output = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Output(
                    pris = int("belop_beregnet").withValuta(valuta),
                ),
            )
        }
    }

    private fun getTilsagnBeregningFriLinjer(tilsagnId: UUID): List<TilsagnBeregningFri.InputLinje> {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_fri_beregning
            where tilsagn_id = ?::uuid
        """.trimIndent()
        return session.list(queryOf(query, tilsagnId)) {
            val valuta = it.string("valuta").let { currencyStr -> Valuta.valueOf(currencyStr) }
            TilsagnBeregningFri.InputLinje(
                id = it.uuid("id"),
                beskrivelse = it.string("beskrivelse"),
                pris = it.int("belop").withValuta(valuta),
                antall = it.int("antall"),
            )
        }
    }
}

fun Session.createArrayOfTilsagnType(
    types: List<TilsagnType>,
): Array = createArrayOf("tilsagn_type", types)

fun Session.createArrayOfTilsagnStatus(
    statuser: List<TilsagnStatus>,
): Array = createArrayOf("tilsagn_status", statuser)
