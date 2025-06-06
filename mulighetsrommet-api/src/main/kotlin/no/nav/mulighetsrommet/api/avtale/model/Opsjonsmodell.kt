package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class Opsjonsmodell(
    val type: OpsjonsmodellType,
    @Serializable(with = LocalDateSerializer::class)
    val opsjonMaksVarighet: LocalDate?,
    val customOpsjonsmodellNavn: String? = null,
)

@Serializable
enum class OpsjonsmodellType {
    TO_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
    INGEN_OPSJONSMULIGHET,
    VALGFRI_SLUTTDATO,
    ANNET,
}
