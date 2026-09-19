package no.nav.mulighetsrommet.api.domain.avtale

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
sealed interface Opsjonsmodell {
    sealed interface MedOpsjonsmulighet : Opsjonsmodell {
        fun maksSluttDato(startDato: LocalDate): LocalDate
    }

    @Serializable
    @SerialName("TO_PLUSS_EN")
    data object ToPlussEn : MedOpsjonsmulighet {
        override fun maksSluttDato(startDato: LocalDate): LocalDate = startDato.plusYears(3).minusDays(1)
    }

    @Serializable
    @SerialName("TO_PLUSS_EN_PLUSS_EN")
    data object ToPlussEnPlussEn : MedOpsjonsmulighet {
        override fun maksSluttDato(startDato: LocalDate): LocalDate = startDato.plusYears(4).minusDays(1)
    }

    @Serializable
    @SerialName("TO_PLUSS_EN_PLUSS_EN_PLUSS_EN")
    data object ToPlussEnPlussEnPlussEn : MedOpsjonsmulighet {
        override fun maksSluttDato(startDato: LocalDate): LocalDate = startDato.plusYears(5).minusDays(1)
    }

    @Serializable
    @SerialName("ANNET")
    data class Annet(
        @Serializable(with = LocalDateSerializer::class)
        val opsjonMaksVarighet: LocalDate,
        val customOpsjonsmodellNavn: String,
    ) : MedOpsjonsmulighet {
        override fun maksSluttDato(startDato: LocalDate): LocalDate = opsjonMaksVarighet
    }

    @Serializable
    @SerialName("VALGFRI_SLUTTDATO")
    data object ValgfriSluttdato : Opsjonsmodell

    @Serializable
    @SerialName("INGEN_OPSJONSMULIGHET")
    data object IngenOpsjonsmulighet : Opsjonsmodell
}
