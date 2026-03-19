package no.nav.mulighetsrommet.api.arrangorflate.dto

import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.utils.Pagination

data class ArrangorflateUtbetalingFilter(
    val sok: String? = null,
    val type: Type = Type.AKTIVE,
    val pagination: Pagination = Pagination.all(),
    val orderBy: OrderBy = OrderBy.ARRANGOR,
    val direction: Direction = Direction.ASC,
) {

    @Serializable
    enum class Type {
        AKTIVE,
        HISTORISKE,
        ;

        fun utbetalingStatuser(): Set<UtbetalingStatusType> = when (this) {
            AKTIVE -> setOf(
                UtbetalingStatusType.GENERERT,
                UtbetalingStatusType.TIL_BEHANDLING,
                UtbetalingStatusType.TIL_ATTESTERING,
                UtbetalingStatusType.RETURNERT,
            )

            HISTORISKE -> setOf(
                UtbetalingStatusType.FERDIG_BEHANDLET,
                UtbetalingStatusType.DELVIS_UTBETALT,
                UtbetalingStatusType.UTBETALT,
                UtbetalingStatusType.AVBRUTT,
            )
        }

        companion object {
            fun from(type: String?): Type = when (type) {
                "AKTIVE" -> AKTIVE
                "HISTORISKE" -> HISTORISKE
                else -> AKTIVE
            }
        }
    }

    @Serializable
    enum class OrderBy {
        TILTAK,
        ARRANGOR,
        PERIODE,
        BELOP,
        STATUS,
        ;

        companion object {
            fun from(str: String?): OrderBy =
                str?.let { OrderBy.valueOf(it) } ?: ARRANGOR
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

fun RoutingContext.getArrangorflateUtbetalingFilter(): ArrangorflateUtbetalingFilter {
    val sok = call.queryParameters["sok"]?.ifBlank { null }
    val type = ArrangorflateUtbetalingFilter.Type.from(call.queryParameters["type"])
    val pagination = when (type) {
        ArrangorflateUtbetalingFilter.Type.AKTIVE -> Pagination.all()
        ArrangorflateUtbetalingFilter.Type.HISTORISKE ->
            getPaginationParams()
    }
    val orderBy = ArrangorflateUtbetalingFilter.OrderBy.from(call.parameters["orderBy"])
    val direction = ArrangorflateUtbetalingFilter.Direction.from(call.parameters["direction"])
    return ArrangorflateUtbetalingFilter(
        sok,
        type,
        pagination,
        orderBy,
        direction,
    )
}
