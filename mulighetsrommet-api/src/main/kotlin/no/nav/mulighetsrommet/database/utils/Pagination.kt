package no.nav.mulighetsrommet.database.utils

class Pagination private constructor(
    private val page: Int,
    val limit: Int? = null,
) {

    init {
        require(page > 0) {
            "'page' must be greater than '0'"
        }

        require(limit == null || limit > 0) {
            "'limit' must be 'null' or greater than '0'"
        }
    }

    val offset: Int?
        get() = if (limit == null) {
            null
        } else {
            (page - 1) * limit
        }

    companion object {
        fun all() = Pagination(
            page = 1,
            limit = null,
        )

        fun of(page: Int, size: Int) = Pagination(
            page = page,
            limit = size,
        )
    }
}
