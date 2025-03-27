package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto

@Serializable
data class TilsagnDetaljerDto(
    val tilsagn: TilsagnDto,
    val opprettelse: TotrinnskontrollDto,
    val annullering: TotrinnskontrollDto?,
    val tilOppgjor: TotrinnskontrollDto?,
)
