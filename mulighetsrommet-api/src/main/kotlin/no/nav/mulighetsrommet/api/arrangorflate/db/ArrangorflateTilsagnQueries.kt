package no.nav.mulighetsrommet.api.arrangorflate.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnFilter
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTilsagnKompakt
import no.nav.mulighetsrommet.api.arrangorflate.model.toArrangorflateTilsagnKompakt
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
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
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
    ): PaginatedResult<ArrangorflateTilsagnKompakt> {
        val direction = when (filter.direction) {
            ArrangorflateFilterDirection.ASC -> "asc"
            ArrangorflateFilterDirection.DESC -> "desc"
        }

        val order = when (filter.orderBy) {
            ArrangorflateTilsagnFilter.OrderBy.TILTAK -> "tiltakstype_navn $direction, gjennomforing_navn $direction"
            ArrangorflateTilsagnFilter.OrderBy.ARRANGOR -> "arrangor_navn $direction, arrangor_organisasjonsnummer $direction"
            ArrangorflateTilsagnFilter.OrderBy.START_DATO -> "lower(periode) $direction"
            ArrangorflateTilsagnFilter.OrderBy.SLUTT_DATO -> "upper(periode) $direction"
            ArrangorflateTilsagnFilter.OrderBy.TILSAGN -> "type $direction"
            ArrangorflateTilsagnFilter.OrderBy.STATUS -> "status $direction"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over() as total_count
            from view_arrangorflate_tilsagn_kompakt
            where
                (:search::text is null or fts @@ to_tsquery('norwegian', :search))
                and arrangor_organisasjonsnummer = any (:orgnr_list::text[])
                and status = any (:status_list::text[])
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()
        val params = mapOf(
            "search" to filter.search?.toFTSPrefixQuery(),
            "orgnr_list" to session.createArrayOfValue(arrangorer) { it.value },
            "status_list" to session.createTextArray(TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR),
        )
        return queryOf(query, params + filter.pagination.parameters)
            .mapPaginated { it.toArrangorflateTilsagnKompakt() }
            .runWithSession(session)
    }
}
