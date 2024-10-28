package no.nav.mulighetsrommet.domain.dto

sealed class AvbruttAarsak {
    abstract val name: String
    abstract val beskrivelse: String

    data object EndringHosArrangor : AvbruttAarsak() {
        override val name = "ENDRING_HOS_ARRANGOR"
        override val beskrivelse = "Endring hos arrangør"
    }
    data object BudsjettHensyn : AvbruttAarsak() {
        override val name = "BUDSJETT_HENSYN"
        override val beskrivelse = "Budsjetthensyn"
    }
    data object ForFaaDeltakere : AvbruttAarsak() {
        override val name = "FOR_FAA_DELTAKERE"
        override val beskrivelse = "For få deltakere"
    }
    data object Feilregistrering : AvbruttAarsak() {
        override val name = "FEILREGISTRERING"
        override val beskrivelse = "Feilregistrering"
    }
    data object AvbruttIArena : AvbruttAarsak() {
        override val name = "AVBRUTT_I_ARENA"
        override val beskrivelse = "Avbrutt i Arena"
    }
    data class Annet(override val name: String) : AvbruttAarsak() {
        override val beskrivelse = name
    }

    companion object {
        fun fromString(value: String): AvbruttAarsak = when (value) {
            "ENDRING_HOS_ARRANGOR" -> EndringHosArrangor
            "BUDSJETT_HENSYN" -> BudsjettHensyn
            "FOR_FAA_DELTAKERE" -> ForFaaDeltakere
            "FEILREGISTRERING" -> Feilregistrering
            "AVBRUTT_I_ARENA" -> AvbruttIArena
            else -> Annet(value)
        }
    }
}
