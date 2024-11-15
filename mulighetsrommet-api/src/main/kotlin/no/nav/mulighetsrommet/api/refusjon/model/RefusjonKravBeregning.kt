package no.nav.mulighetsrommet.api.refusjon.model

import java.security.MessageDigest

sealed class RefusjonKravBeregning {
    abstract val input: RefusjonKravBeregningInput
    abstract val output: RefusjonKravBeregningOutput

    @OptIn(ExperimentalStdlibApi::class)
    fun getDigest(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toString().toByteArray() + output.toString().toByteArray())
        return digest.toHexString()
    }
}

abstract class RefusjonKravBeregningInput {
    abstract val periode: RefusjonskravPeriode
}

abstract class RefusjonKravBeregningOutput {
    abstract val belop: Int
}
