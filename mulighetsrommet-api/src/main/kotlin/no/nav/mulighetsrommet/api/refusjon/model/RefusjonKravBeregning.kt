package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.Serializable

@Serializable
sealed class RefusjonKravBeregning {
    abstract val input: RefusjonKravBeregningInput
    abstract val output: RefusjonKravBeregningOutput

    @OptIn(ExperimentalStdlibApi::class)
    fun getDigest(): String {
        return (input.hashCode() + output.hashCode()).toHexString()
    }
}

abstract class RefusjonKravBeregningInput

abstract class RefusjonKravBeregningOutput {
    abstract val belop: Int
}
