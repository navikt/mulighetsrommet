package no.nav.mulighetsrommet.api.refusjon.model

sealed class RefusjonKravBeregning {
    abstract val input: RefusjonKravBeregningInput
    abstract val output: RefusjonKravBeregningOutput

    @OptIn(ExperimentalStdlibApi::class)
    fun getDigest(): String {
        return (input.hashCode() + output.hashCode()).toHexString()
    }
}

abstract class RefusjonKravBeregningInput {
    abstract val periode: RefusjonskravPeriode
}

abstract class RefusjonKravBeregningOutput {
    abstract val belop: Int
}
