package no.nav.mulighetsrommet.api.arrangorflate.dto

import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.database.utils.Pagination

data class ArrangorflateTilsagnFilter(
    val sok: String? = null,
    val pagination: Pagination = Pagination.all(),
    val orderBy: OrderBy = OrderBy.PERIODE,
    val direction: Direction = Direction.DESC,
) {

    @Serializable
    enum class OrderBy {
        TILTAK,
        ARRANGOR,
        PERIODE,
        TILSAGN,
        STATUS,
        ;

        companion object {
            fun from(str: String?): OrderBy = str?.let { OrderBy.valueOf(it) } ?: ARRANGOR
        }
    }

    @Serializable
    enum class Direction {
        ASC,
        DESC,
        ;

        companion object {
            fun from(str: String?): Direction = str?.let { Direction.valueOf(it) } ?: ASC
        }
    }
}

fun RoutingContext.getArrangorflateTilsagnFilter(): ArrangorflateTilsagnFilter {
    val sok = call.queryParameters["sok"]?.ifBlank { null }
    val pagination = getPaginationParams()
    val orderBy = ArrangorflateTilsagnFilter.OrderBy.from(call.parameters["orderBy"])
    val direction = ArrangorflateTilsagnFilter.Direction.from(call.parameters["direction"])
    return ArrangorflateTilsagnFilter(
        sok,
        pagination,
        orderBy,
        direction,
    )
}
