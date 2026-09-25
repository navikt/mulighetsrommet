package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.model.ValutaBelop

@Serializable
sealed class TilsagnBeregning {
    abstract val input: TilsagnBeregningInput
    abstract val output: TilsagnBeregningOutput
}

@Serializable
sealed class TilsagnBeregningInput {
    /**
     * Prismodellen på gjennomføringen på det tidspunktet tilsagnet ble beregnet.
     */
    abstract val prismodell: PrismodellType
}

@Serializable
sealed class TilsagnBeregningOutput {
    abstract val pris: ValutaBelop
}
