package no.nav.mulighetsrommet.domain.dto

sealed class AvbruttAarsak {
    abstract val name: String

    object EndringHosArrangor : AvbruttAarsak() {
        override val name = "ENDRING_HOS_ARRANGOR"
    }
    object BudsjettHensyn : AvbruttAarsak() {
        override val name = "BUDSJETT_HENSYN"
    }
    object ForFaaDeltakere : AvbruttAarsak() {
        override val name = "FOR_FAA_DELTAKERE"
    }
    object Feilregistrering : AvbruttAarsak() {
        override val name = "FEILREGISTRERING"
    }
    object AvbruttIArena : AvbruttAarsak() {
        override val name = "AVBRUTT_I_ARENA"
    }
    data class Annet(override val name: String) : AvbruttAarsak()
}
