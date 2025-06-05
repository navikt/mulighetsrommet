package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
enum class Opsjonsmodell {
    TO_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN,
    TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
    AVTALE_UTEN_OPSJONSMODELL,
    AVTALE_VALGFRI_SLUTTDATO,
    ANNET,
}

@Serializable
data class OpsjonsmodellData(
    @Serializable(with = LocalDateSerializer::class)
    val opsjonMaksVarighet: LocalDate?,
    val opsjonsmodell: Opsjonsmodell?,
    val customOpsjonsmodellNavn: String? = null,
)
