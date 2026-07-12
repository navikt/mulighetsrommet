package no.nav.mulighetsrommet.database.utils

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

    val offset: Int?
        get() = pageSize?.let { (page - 1) * it }

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

    override fun toString(): String {
        return "Pagination(page=$page, pageSize=$pageSize)"
    }
}

data class PaginatedResult<T>(val totalCount: Int, val items: List<T>)

inline fun <T, U> PaginatedResult<T>.map(transform: (T) -> U): PaginatedResult<U> {
    return PaginatedResult(totalCount, items.map(transform))
}
