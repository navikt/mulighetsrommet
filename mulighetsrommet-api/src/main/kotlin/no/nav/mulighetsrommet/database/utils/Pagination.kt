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
