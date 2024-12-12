package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable

@Serializable
enum class TilsagnStatusAarsak {
    FEIL_REGISTRERING,
    FEIL_ANTALL_PLASSER,
    FEIL_KOSTNADSSTED,
    FEIL_PERIODE,
    FEIL_BELOP,
    FEIL_ANNET,
}
