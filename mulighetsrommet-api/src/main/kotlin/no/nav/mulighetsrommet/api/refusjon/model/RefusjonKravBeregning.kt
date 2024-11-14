package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
sealed class RefusjonKravBeregning {
    abstract val input: RefusjonKravBeregningInput
    abstract val output: RefusjonKravBeregningOutput
}

abstract class RefusjonKravBeregningInput {
    abstract val periodeStart: LocalDate
    abstract val periodeSlutt: LocalDate
}

abstract class RefusjonKravBeregningOutput {
    abstract val belop: Int
}
