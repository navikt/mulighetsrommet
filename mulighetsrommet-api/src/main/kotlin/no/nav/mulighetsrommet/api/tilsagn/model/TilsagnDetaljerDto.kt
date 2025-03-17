package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll

@Serializable
data class TilsagnDetaljerDto(
    val tilsagn: TilsagnDto,
    val opprettelse: Totrinnskontroll,
    val annullering: Totrinnskontroll?,
    val frigjoring: Totrinnskontroll?,
)
