package no.nav.mulighetsrommet.database.utils

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.action.QueryAction

class Pagination private constructor(
    private val page: Int,
    val pageSize: Int? = null,
) {

    init {
        require(page > 0) {
            "'page' must be greater than '0'"
        }

        require(pageSize == null || pageSize > 0) {
            "'pageSize' must be 'null' or greater than '0'"
        }
    }

    companion object {
        fun all() = Pagination(
            page = 1,
            pageSize = null,
        )

        fun of(page: Int, size: Int) = Pagination(
            page = page,
            pageSize = size,
        )
    }

    val parameters: Map<String, Int?>
        get() {
            val offset = if (pageSize == null) {
                null
            } else {
                (page - 1) * pageSize
            }

            return mapOf(
                "limit" to pageSize,
                "offset" to offset,
            )
        }

    override fun toString(): String {
        return "Pagination(page=$page, pageSize=$pageSize)"
    }
}

data class PaginatedResult<T>(val totalCount: Int, val items: List<T>)

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
