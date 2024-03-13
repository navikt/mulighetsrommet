package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.NavIdent

@Serializable
data class VeilederJoyrideDto(
    val navIdent: NavIdent,
    val fullfort: Boolean,
    val type: JoyrideType,
)

@Serializable
data class VeilederJoyrideRequest(
    val fullfort: Boolean,
    val joyrideType: JoyrideType,
)

@Serializable
enum class JoyrideType {
    OVERSIKT,
    DETALJER,
    HAR_VIST_OPPRETT_AVTALE,
}
