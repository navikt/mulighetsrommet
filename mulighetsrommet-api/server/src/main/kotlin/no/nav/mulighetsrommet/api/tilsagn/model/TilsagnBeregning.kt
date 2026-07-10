package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.ValutaBelop

@Serializable
sealed class TilsagnBeregning {
    abstract val input: TilsagnBeregningInput
    abstract val output: TilsagnBeregningOutput
}

@Serializable
sealed class TilsagnBeregningInput

@Serializable
sealed class TilsagnBeregningOutput {
    abstract val pris: ValutaBelop
}
