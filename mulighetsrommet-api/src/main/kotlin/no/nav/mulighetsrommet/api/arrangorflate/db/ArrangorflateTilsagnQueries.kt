package no.nav.mulighetsrommet.api.arrangorflate.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnFilter
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTilsagnKompakt
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.mapPaginated
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
