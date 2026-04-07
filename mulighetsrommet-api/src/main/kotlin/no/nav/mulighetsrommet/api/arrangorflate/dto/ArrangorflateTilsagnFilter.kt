package no.nav.mulighetsrommet.api.arrangorflate.dto

import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.database.utils.Pagination

data class ArrangorflateTilsagnFilter(
    val pagination: Pagination,
    val orderBy: OrderBy,
    val direction: ArrangorflateFilterDirection,
) {
    @Serializable
    enum class OrderBy {
        TILTAK,
        ARRANGOR,
        PERIODE,
        TILSAGN,
        STATUS,
    }
}

fun RoutingContext.getArrangorflateTilsagnFilter(): ArrangorflateTilsagnFilter {
    val pagination = getPaginationParams()
    val orderBy = call.parameters["orderBy"]?.let {
        ArrangorflateTilsagnFilter.OrderBy.valueOf(it)
    } ?: ArrangorflateTilsagnFilter.OrderBy.PERIODE
    val direction = ArrangorflateFilterDirection.from(call.parameters["direction"])

    return ArrangorflateTilsagnFilter(
        pagination,
        orderBy,
        direction,
    )
}
