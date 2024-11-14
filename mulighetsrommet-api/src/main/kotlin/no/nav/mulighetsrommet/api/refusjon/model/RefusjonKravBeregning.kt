package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.Serializable

@Serializable
sealed class RefusjonKravBeregning {
    abstract val input: RefusjonKravBeregningInput
    abstract val output: RefusjonKravBeregningOutput
}

abstract class RefusjonKravBeregningInput {
    abstract val periode: RefusjonskravPeriode
}

abstract class RefusjonKravBeregningOutput {
    abstract val belop: Int
}
