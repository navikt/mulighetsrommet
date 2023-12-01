package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class VeilederJoyrideDto(
    val navident: String,
    val fullfort: Boolean,
    val type: JoyrideType,
)

@Serializable
data class VeilederJoyrideRequest(
    val fullfort: Boolean,
    val type: JoyrideType,
)

@Serializable
data class HarFullfortJoyrideRequest(
    val type: JoyrideType,
)

@Serializable
enum class JoyrideType {
    OVERSIKT,
    OVERSIKTEN_LAST_STEP,
    DETALJER,
    HAR_VIST_OPPRETT_AVTALE,
}
