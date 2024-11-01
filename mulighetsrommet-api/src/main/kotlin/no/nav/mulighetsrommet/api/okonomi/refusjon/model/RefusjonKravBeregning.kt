package no.nav.mulighetsrommet.api.okonomi.refusjon.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
sealed class RefusjonKravBeregning {
    abstract val input: RefusjonKravBeregningInput
    abstract val output: RefusjonKravBeregningOutput
}

abstract class RefusjonKravBeregningInput {
    abstract val periodeStart: LocalDateTime
    abstract val periodeSlutt: LocalDateTime
}

abstract class RefusjonKravBeregningOutput {
    abstract val belop: Int
}
