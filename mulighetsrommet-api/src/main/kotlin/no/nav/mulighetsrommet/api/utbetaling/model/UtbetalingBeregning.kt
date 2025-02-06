package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable

@Serializable
sealed class UtbetalingBeregning {
    abstract val input: UtbetalingBeregningInput
    abstract val output: UtbetalingBeregningOutput

    @OptIn(ExperimentalStdlibApi::class)
    fun getDigest(): String {
        return (input.hashCode() + output.hashCode()).toHexString()
    }
}

abstract class UtbetalingBeregningInput

abstract class UtbetalingBeregningOutput {
    abstract val belop: Int
}
