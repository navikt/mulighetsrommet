package no.nav.mulighetsrommet.api.okonomi.models

import kotlinx.serialization.Serializable

@Serializable
sealed class RefusjonKravBeregning {
    abstract val input: RefusjonKravBeregningInput
    abstract val output: RefusjonKravBeregningOutput
}

abstract class RefusjonKravBeregningInput

abstract class RefusjonKravBeregningOutput {
    abstract val belop: Int
}
