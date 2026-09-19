package no.nav.mulighetsrommet.api.domain.avtale

import arrow.core.Either
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.validation
import java.time.LocalDate

/**
 * En [Opsjonsmodell] avgjør hvordan sluttdatoen til en avtale kan endres over tid:
 * - [ValgfriSluttdato] og [IngenOpsjonsmulighet] eier sin egen sluttdato og har ingen opsjonsmulighet.
 * - Undertyper av [MedOpsjonsmulighet] har en maks varighet avtalen kan forlenges til gjennom å registrere
 *   opsjoner (se [Avtale.registrerOpsjon]), og kan ikke endre sluttdato direkte.
 */
@Serializable
sealed class Opsjonsmodell {
    abstract val type: OpsjonsmodellType

    @Serializable
    @SerialName("VALGFRI_SLUTTDATO")
    data class ValgfriSluttdato(
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
    ) : Opsjonsmodell() {
        @Transient
        override val type = OpsjonsmodellType.VALGFRI_SLUTTDATO
    }

    @Serializable
    @SerialName("INGEN_OPSJONSMULIGHET")
    data class IngenOpsjonsmulighet(
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate,
    ) : Opsjonsmodell() {
        @Transient
        override val type = OpsjonsmodellType.INGEN_OPSJONSMULIGHET
    }

    sealed interface MedOpsjonsmulighet {
        val opsjonMaksVarighet: LocalDate
    }

    @Serializable
    @SerialName("TO_PLUSS_EN")
    data class ToPlussEn private constructor(
        @Serializable(with = LocalDateSerializer::class)
        private val startDato: LocalDate,
    ) : Opsjonsmodell(), MedOpsjonsmulighet {
        @Transient
        override val type = OpsjonsmodellType.TO_PLUSS_EN

        override val opsjonMaksVarighet: LocalDate
            get() = startDato.plusYears(3).minusDays(1)

        companion object {
            fun forStartDato(startDato: LocalDate) = ToPlussEn(startDato)
        }
    }

    @Serializable
    @SerialName("TO_PLUSS_EN_PLUSS_EN")
    data class ToPlussEnPlussEn private constructor(
        @Serializable(with = LocalDateSerializer::class)
        private val startDato: LocalDate,
    ) : Opsjonsmodell(), MedOpsjonsmulighet {
        @Transient
        override val type = OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN

        override val opsjonMaksVarighet: LocalDate
            get() = startDato.plusYears(4).minusDays(1)

        companion object {
            fun forStartDato(startDato: LocalDate) = ToPlussEnPlussEn(startDato)
        }
    }

    @Serializable
    @SerialName("TO_PLUSS_EN_PLUSS_EN_PLUSS_EN")
    data class ToPlussEnPlussEnPlussEn private constructor(
        @Serializable(with = LocalDateSerializer::class)
        private val startDato: LocalDate,
    ) : Opsjonsmodell(), MedOpsjonsmulighet {
        @Transient
        override val type = OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN

        override val opsjonMaksVarighet: LocalDate
            get() = startDato.plusYears(5).minusDays(1)

        companion object {
            fun forStartDato(startDato: LocalDate) = ToPlussEnPlussEnPlussEn(startDato)
        }
    }

    @Serializable
    @SerialName("ANNET")
    data class Annet(
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        override val opsjonMaksVarighet: LocalDate,
        val customOpsjonsmodellNavn: String,
    ) : Opsjonsmodell(), MedOpsjonsmulighet {
        @Transient
        override val type = OpsjonsmodellType.ANNET
    }

    fun initiellSluttDato(startDato: LocalDate): LocalDate? = when (this) {
        is ValgfriSluttdato -> sluttDato
        is IngenOpsjonsmulighet -> sluttDato
        is Annet -> sluttDato
        is ToPlussEn, is ToPlussEnPlussEn, is ToPlussEnPlussEnPlussEn -> startDato.plusYears(2).minusDays(1)
    }

    val opsjonMaksVarighetOrNull: LocalDate?
        get() = (this as? MedOpsjonsmulighet)?.opsjonMaksVarighet

    val customOpsjonsmodellNavnOrNull: String?
        get() = (this as? Annet)?.customOpsjonsmodellNavn

    companion object {
        fun of(
            type: OpsjonsmodellType,
            startDato: LocalDate,
            sluttDato: LocalDate?,
            opsjonMaksVarighet: LocalDate?,
            customOpsjonsmodellNavn: String?,
        ): Either<List<FieldError>, Opsjonsmodell> = validation {
            when (type) {
                OpsjonsmodellType.VALGFRI_SLUTTDATO -> ValgfriSluttdato(sluttDato)

                OpsjonsmodellType.INGEN_OPSJONSMULIGHET -> {
                    requireNotNull(sluttDato) {
                        FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")
                    }
                    IngenOpsjonsmulighet(sluttDato)
                }

                OpsjonsmodellType.TO_PLUSS_EN -> ToPlussEn.forStartDato(startDato)

                OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN -> ToPlussEnPlussEn.forStartDato(startDato)

                OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN -> ToPlussEnPlussEnPlussEn.forStartDato(startDato)

                OpsjonsmodellType.ANNET -> {
                    validateNotNull(sluttDato) {
                        FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")
                    }
                    validateNotNull(opsjonMaksVarighet) {
                        FieldError("/opsjonsmodell", "Du må legge inn maks varighet for opsjonen")
                    }
                    requireValid(!customOpsjonsmodellNavn.isNullOrBlank()) {
                        FieldError("/opsjonsmodell", "Du må beskrive opsjonsmodellen")
                    }
                    Annet(
                        sluttDato = requireNotNull(sluttDato),
                        opsjonMaksVarighet = requireNotNull(opsjonMaksVarighet),
                        customOpsjonsmodellNavn = customOpsjonsmodellNavn,
                    )
                }
            }
        }
    }
}

@Serializable
enum class OpsjonsmodellType {
    TO_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
    INGEN_OPSJONSMULIGHET,
    VALGFRI_SLUTTDATO,
    ANNET,
}
