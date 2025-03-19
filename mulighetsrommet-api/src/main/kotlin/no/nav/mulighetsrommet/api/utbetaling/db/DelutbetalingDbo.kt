package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.FakturaStatusType
import java.util.*

data class DelutbetalingDbo(
    val id: UUID,
    val tilsagnId: UUID,
    val utbetalingId: UUID,
    val status: DelutbetalingStatus,
    val belop: Int,
    val frigjorTilsagn: Boolean,
    val periode: Periode,
    val lopenummer: Int,
    val fakturanummer: String,
    val fakturastatus: FakturaStatusType?,
)
