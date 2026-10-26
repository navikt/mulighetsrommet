package no.nav.mulighetsrommet.admin.avtale

import arrow.core.Either
import no.nav.mulighetsrommet.api.domain.avtale.Opsjonsmodell
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.validation
import java.time.LocalDate

data class OpsjonsmodellInput(
    val type: OpsjonsmodellType,
    val opsjonMaksVarighet: LocalDate?,
    val customOpsjonsmodellNavn: String?,
) {
    fun toDomain(): Either<List<FieldError>, Opsjonsmodell> = validation {
        when (type) {
            OpsjonsmodellType.VALGFRI_SLUTTDATO -> Opsjonsmodell.ValgfriSluttdato

            OpsjonsmodellType.INGEN_OPSJONSMULIGHET -> Opsjonsmodell.IngenOpsjonsmulighet

            OpsjonsmodellType.TO_PLUSS_EN -> Opsjonsmodell.ToPlussEn

            OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN -> Opsjonsmodell.ToPlussEnPlussEn

            OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN ->
                Opsjonsmodell.ToPlussEnPlussEnPlussEn

            OpsjonsmodellType.ANNET -> {
                validateNotNull(opsjonMaksVarighet) {
                    FieldError("/opsjonsmodell", "Du må legge inn maks varighet for opsjonen")
                }
                requireValid(!customOpsjonsmodellNavn.isNullOrBlank()) {
                    FieldError("/opsjonsmodell", "Du må beskrive opsjonsmodellen")
                }
                Opsjonsmodell.Annet(
                    opsjonMaksVarighet = requireNotNull(opsjonMaksVarighet),
                    customOpsjonsmodellNavn = customOpsjonsmodellNavn,
                )
            }
        }
    }
}

enum class OpsjonsmodellType {
    TO_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
    INGEN_OPSJONSMULIGHET,
    VALGFRI_SLUTTDATO,
    ANNET,
}

fun Opsjonsmodell.toType(): OpsjonsmodellType = when (this) {
    is Opsjonsmodell.ToPlussEn -> OpsjonsmodellType.TO_PLUSS_EN
    is Opsjonsmodell.ToPlussEnPlussEn -> OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN
    is Opsjonsmodell.ToPlussEnPlussEnPlussEn -> OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN
    is Opsjonsmodell.IngenOpsjonsmulighet -> OpsjonsmodellType.INGEN_OPSJONSMULIGHET
    is Opsjonsmodell.ValgfriSluttdato -> OpsjonsmodellType.VALGFRI_SLUTTDATO
    is Opsjonsmodell.Annet -> OpsjonsmodellType.ANNET
}
