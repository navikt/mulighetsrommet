package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import java.util.*

data class DelutbetalingDbo(
    val tilsagnId: UUID,
    val utbetalingId: UUID,
    val belop: Int,
    val periode: Periode,
    val opprettetAv: NavIdent,
)
