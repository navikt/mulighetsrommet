package no.nav.mulighetsrommet.database.utils

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.action.QueryAction

/**
 * Sql-parametre for [Pagination], navngitt for å matche `limit`/`offset` i en parameterisert spørring.
 */
val Pagination.parameters: Map<String, Int?>
    get() = mapOf(
        "limit" to pageSize,
        "offset" to offset,
    )

/**
 * Utility som utvider [Query] med en accessor for paginert data.
 *
 * Det forventes at resultatet av [Query]-instansen inkluderer en kolonne med navn `total_count`,
 * evt. kan dette overstyres via [totalCountColumn], da dette blir hentet ut fra underliggende [extractor]
 * som del av [PaginatedListResultQueryAction].
 */
fun <A> Query.mapPaginated(
    totalCountColumn: String = "total_count",
    extractor: (Row) -> A,
): PaginatedListResultQueryAction<A> {
    return PaginatedListResultQueryAction(this, totalCountColumn, extractor)
}

data class PaginatedListResultQueryAction<A>(
    val query: Query,
    val totalCountColumn: String,
    val extractor: (Row) -> A?,
) : QueryAction<PaginatedResult<A>> {

    override fun runWithSession(session: Session): PaginatedResult<A> {
        var firstTotalCount: Int? = null

        val items = session.list(query) { row ->
            if (firstTotalCount == null) {
                firstTotalCount = row.intOrNull(totalCountColumn)
            }

            extractor(row)
        }

        val totalCount = firstTotalCount ?: 0

        return PaginatedResult(totalCount, items)
    }
}
