package no.nav.mulighetsrommet.api.utils

class PaginationParams(
    page: Int?,
    limit: Int?
) {
    val page: Int
    private val size: Int
    val offset get() = (page - 1) * size
    val limit get() = size

    init {
        this.page = page ?: 1
        this.size = limit ?: 50
    }
}
