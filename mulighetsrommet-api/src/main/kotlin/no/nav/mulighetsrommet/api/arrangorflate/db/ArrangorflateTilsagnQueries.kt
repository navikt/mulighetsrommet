package no.nav.mulighetsrommet.api.arrangorflate.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnFilter
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
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.BestillingStatusType
import org.intellij.lang.annotations.Language
import java.util.UUID

val TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR = listOf(
    TilsagnStatus.GODKJENT,
    TilsagnStatus.TIL_ANNULLERING,
    TilsagnStatus.ANNULLERT,
    TilsagnStatus.OPPGJORT,
    TilsagnStatus.TIL_OPPGJOR,
)

class ArrangorflateTilsagnQueries(val session: Session) {

    fun getFiltered(
        arrangorer: Set<Organisasjonsnummer>,
        filter: ArrangorflateTilsagnFilter,
    ): PaginatedResult<Tilsagn> {
        val direction = when (filter.direction) {
            ArrangorflateFilterDirection.ASC -> "asc"
            ArrangorflateFilterDirection.DESC -> "desc"
        }

        val order = when (filter.orderBy) {
            ArrangorflateTilsagnFilter.OrderBy.TILTAK -> "tiltakstype_navn $direction, gjennomforing_navn $direction"
            ArrangorflateTilsagnFilter.OrderBy.ARRANGOR -> "arrangor_navn $direction, arrangor_organisasjonsnummer $direction"
            ArrangorflateTilsagnFilter.OrderBy.PERIODE -> "periode $direction"
            ArrangorflateTilsagnFilter.OrderBy.TILSAGN -> "type $direction"
            ArrangorflateTilsagnFilter.OrderBy.STATUS -> "status $direction"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over() as total_count
            from view_tilsagn
            where
                arrangor_organisasjonsnummer = any (:orgnr_list::text[])
                and status = any (:status_list::text[])
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()
        val params = mapOf(
            "orgnr_list" to session.createArrayOfValue(arrangorer) { it.value },
            "status_list" to session.createTextArray(TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR),
        )
        return queryOf(query, params + filter.pagination.parameters)
            .mapPaginated { it.toTilsagn() }
            .runWithSession(session)
    }

    private fun Row.toTilsagn(): Tilsagn {
        val id = uuid("id")
        val valuta = string("valuta").let { Valuta.valueOf(it) }

        val beregning = getBeregning(id, valuta, TilsagnBeregningType.valueOf(string("beregning_type")))
        val deltakere = stringOrNull("deltakere")
            ?.let { Json.decodeFromString<List<Tilsagn.Deltaker>>(it) }
            ?: emptyList()

        return Tilsagn(
            id = uuid("id"),
            type = TilsagnType.valueOf(string("tilsagn_type")),
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
            deltakere = deltakere,
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
