package no.nav.mulighetsrommet.api.arrangorflate.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnFilter
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTilsagnKompakt
import no.nav.mulighetsrommet.api.shared.PaginatedResult
import no.nav.mulighetsrommet.api.shared.Pagination
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.database.utils.parameters
import no.nav.mulighetsrommet.database.utils.toFTSPrefixQuery
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import org.intellij.lang.annotations.Language

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
        search: String? = null,
        pagination: Pagination = Pagination.all(),
        orderBy: ArrangorflateTilsagnFilter.OrderBy = ArrangorflateTilsagnFilter.OrderBy.SLUTT_DATO,
        direction: ArrangorflateFilterDirection = ArrangorflateFilterDirection.ASC,
    ): PaginatedResult<ArrangorflateTilsagnKompakt> {
        val dir = when (direction) {
            ArrangorflateFilterDirection.ASC -> "asc"
            ArrangorflateFilterDirection.DESC -> "desc"
        }

        val order = when (orderBy) {
            ArrangorflateTilsagnFilter.OrderBy.TILTAK -> "tiltakstype_navn $dir, gjennomforing_navn $dir"
            ArrangorflateTilsagnFilter.OrderBy.ARRANGOR -> "arrangor_navn $dir, arrangor_organisasjonsnummer $dir"
            ArrangorflateTilsagnFilter.OrderBy.START_DATO -> "lower(periode) $dir"
            ArrangorflateTilsagnFilter.OrderBy.SLUTT_DATO -> "upper(periode) $dir"
            ArrangorflateTilsagnFilter.OrderBy.TILSAGN -> "tilsagn_type $dir"
            ArrangorflateTilsagnFilter.OrderBy.STATUS -> "status $dir"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over() as total_count
            from view_arrangorflate_tilsagn_kompakt
            where
                (:search::text is null
                    or (
                    fts @@ to_tsquery('norwegian', :search)
                    or gjennomforing_fts @@ to_tsquery('norwegian', :search)
                    )
                )
                and arrangor_organisasjonsnummer = any (:orgnr_list::text[])
                and status = any (:status_list::text[])
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()
        val params = mapOf(
            "search" to search?.toFTSPrefixQuery(),
            "orgnr_list" to session.createArrayOfValue(arrangorer) { it.value },
            "status_list" to session.createTextArray(TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR),
        )
        return queryOf(query, params + pagination.parameters)
            .mapPaginated { it.toArrangorflateTilsagnKompakt() }
            .runWithSession(session)
    }

    fun Row.toArrangorflateTilsagnKompakt(): ArrangorflateTilsagnKompakt = ArrangorflateTilsagnKompakt(
        id = uuid("id"),
        type = TilsagnType.valueOf(string("tilsagn_type")),
        tiltakstype = ArrangorflateTilsagnKompakt.Tiltakstype(
            tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            navn = string("tiltakstype_navn"),
        ),
        gjennomforing = ArrangorflateTilsagnKompakt.Gjennomforing(
            lopenummer = Tiltaksnummer(string("gjennomforing_lopenummer")),
            navn = string("gjennomforing_navn"),
        ),
        periode = periode("periode"),
        bestillingsnummer = string("bestillingsnummer"),
        arrangor = ArrangorflateTilsagnKompakt.Arrangor(
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            navn = string("arrangor_navn"),
        ),
        status = TilsagnStatus.valueOf(string("status")),
    )
}
