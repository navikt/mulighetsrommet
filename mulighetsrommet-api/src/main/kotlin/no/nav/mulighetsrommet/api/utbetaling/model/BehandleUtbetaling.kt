package no.nav.mulighetsrommet.api.utbetaling.model

import java.util.*

data class BehandleUtbetaling(
    val utbetalingId: UUID,
    val kostnadsforderling: List<Kostnad>,
) {
    data class Kostnad(
        val tilsagnId: UUID,
        val belop: Int,
    )
}
