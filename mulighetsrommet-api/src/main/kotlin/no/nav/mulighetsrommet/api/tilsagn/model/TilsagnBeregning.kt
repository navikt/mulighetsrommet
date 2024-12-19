package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable

@Serializable
sealed class TilsagnBeregning {
    abstract val input: TilsagnBeregningInput
    abstract val output: TilsagnBeregningOutput
}

@Serializable
sealed class TilsagnBeregningInput

@Serializable
sealed class TilsagnBeregningOutput {
    abstract val belop: Int
}
