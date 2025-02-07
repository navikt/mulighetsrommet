package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.model.NavIdent
import java.util.*

data class DelutbetalingDbo(
    val tilsagnId: UUID,
    val utbetalingId: UUID,
    val opprettetAv: NavIdent,
    val belop: Int,
)
