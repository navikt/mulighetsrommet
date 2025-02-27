package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import java.util.*

data class DelutbetalingDbo(
    val id: UUID,
    val tilsagnId: UUID,
    val utbetalingId: UUID,
    val belop: Int,
    val periode: Periode,
    val lopenummer: Int,
    val fakturanummer: String,
    val opprettetAv: NavIdent,
)
