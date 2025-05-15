package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri.Input
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningInput

@Serializable
class TilsagnBeregningFriInputRequest(
    val belop: Int,
) {
    fun toTilsagnBeregningFriInput(): TilsagnBeregningInput = Input(belop = belop, prisbetingelser = null)
}
