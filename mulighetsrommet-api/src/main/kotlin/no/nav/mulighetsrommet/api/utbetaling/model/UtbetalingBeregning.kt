package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
sealed class UtbetalingBeregning {
    abstract val input: UtbetalingBeregningInput
    abstract val output: UtbetalingBeregningOutput

    fun getDigest(): String {
        return (input.hashCode() + output.hashCode()).toHexString()
    }
}

sealed class UtbetalingBeregningInput

sealed class UtbetalingBeregningOutput {
    abstract val belop: Int
    abstract val deltakelser: Set<UtbetalingBeregningDeltakelse>
}

sealed class UtbetalingBeregningDeltakelse {
    abstract val deltakelseId: UUID
}
