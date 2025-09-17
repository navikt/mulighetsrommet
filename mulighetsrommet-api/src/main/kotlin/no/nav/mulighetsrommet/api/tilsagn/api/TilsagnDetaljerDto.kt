package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto

@Serializable
data class TilsagnDetaljerDto(
    val tilsagn: TilsagnDto,
    val beregning: TilsagnBeregningDto,
    val opprettelse: TotrinnskontrollDto,
    val annullering: TotrinnskontrollDto?,
    val tilOppgjor: TotrinnskontrollDto?,
    val handlinger: Set<TilsagnHandling>,
)

@Serializable
enum class TilsagnHandling {
    REDIGER,
    SLETT,
    ANNULLER,
    GJOR_OPP,
    GODKJENN,
    RETURNER,
    AVSLA_ANNULLERING,
    GODKJENN_ANNULLERING,
    AVSLA_OPPGJOR,
    GODKJENN_OPPGJOR,
}
