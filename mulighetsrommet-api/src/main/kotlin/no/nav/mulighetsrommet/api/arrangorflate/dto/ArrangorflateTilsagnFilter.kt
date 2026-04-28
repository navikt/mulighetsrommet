package no.nav.mulighetsrommet.api.arrangorflate.dto

import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.database.utils.Pagination

data class ArrangorflateTilsagnFilter(
    val search: String?,
    val pagination: Pagination,
    val orderBy: OrderBy,
    val direction: ArrangorflateFilterDirection,
) {
    @Serializable
    enum class OrderBy {
        TILTAK,
        ARRANGOR,
        START_DATO,
        SLUTT_DATO,
        TILSAGN,
        STATUS,
    }
}

fun RoutingContext.getArrangorflateTilsagnFilter(): ArrangorflateTilsagnFilter {
    val search = call.parameters["search"]?.takeIf { it.isNotBlank() }
    val pagination = getPaginationParams()
    val orderBy = call.parameters["orderBy"]?.let {
        ArrangorflateTilsagnFilter.OrderBy.valueOf(it)
    } ?: ArrangorflateTilsagnFilter.OrderBy.SLUTT_DATO
    val direction = ArrangorflateFilterDirection.from(call.parameters["direction"])

    return ArrangorflateTilsagnFilter(
        search,
        pagination,
        orderBy,
        direction,
    )
}
