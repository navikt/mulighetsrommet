package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.model.ValutaBelop
import java.util.UUID

data class OpprettDelutbetaling(
    val id: UUID,
    val pris: ValutaBelop?,
    val gjorOppTilsagn: Boolean,
    val tilsagn: Tilsagn,
) {
    data class Tilsagn(
        val status: TilsagnStatus,
        val gjenstaendeBelop: ValutaBelop,
    )
}
