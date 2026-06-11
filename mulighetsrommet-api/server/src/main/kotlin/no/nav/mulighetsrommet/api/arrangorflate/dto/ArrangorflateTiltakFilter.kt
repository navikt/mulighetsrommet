package no.nav.mulighetsrommet.api.arrangorflate.dto

import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.database.utils.Pagination
import java.time.LocalDate

data class ArrangorflateTiltakFilter(
    val sok: String? = null,
    val type: ArrangorflateFilterType = ArrangorflateFilterType.AKTIVE,
    val pagination: Pagination = Pagination.all(),
    val orderBy: OrderBy = OrderBy.ARRANGOR,
    val direction: ArrangorflateFilterDirection = ArrangorflateFilterDirection.ASC,
    val sluttDatoGreaterThanOrEqualTo: LocalDate? = null,
) {
    @Serializable
    enum class OrderBy {
        TILTAK,
        ARRANGOR,
        START_DATO,
        SLUTT_DATO,
        STATUS,
        ;

        companion object {
            fun from(str: String?): OrderBy = str?.let { OrderBy.valueOf(it) } ?: ARRANGOR
        }
    }

    companion object {
        fun sluttDatoCutoffDate(): LocalDate = LocalDate.now().minusYears(3).let { LocalDate.of(it.year, 1, 1) }
    }
}

fun RoutingContext.getArrangorflateGjennomforingFilter(): ArrangorflateTiltakFilter {
    val sok = call.queryParameters["sok"]?.ifBlank { null }
    val type = ArrangorflateFilterType.from(call.queryParameters["type"])
    val pagination = when (type) {
        ArrangorflateFilterType.AKTIVE -> Pagination.all()

        ArrangorflateFilterType.HISTORISKE ->
            getPaginationParams()
    }
    val orderBy = ArrangorflateTiltakFilter.OrderBy.from(call.parameters["orderBy"])
    val direction = ArrangorflateFilterDirection.from(call.parameters["direction"])
    return ArrangorflateTiltakFilter(
        sok,
        type,
        pagination,
        orderBy,
        direction,
        sluttDatoGreaterThanOrEqualTo = ArrangorflateTiltakFilter.sluttDatoCutoffDate(),
    )
}
