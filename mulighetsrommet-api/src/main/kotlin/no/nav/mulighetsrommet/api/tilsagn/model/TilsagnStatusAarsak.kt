package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable

@Serializable
enum class TilsagnStatusAarsak {
    FEIL_REGISTRERING,
    TILTAK_SKAL_IKKE_GJENNOMFORES,
    ARRANGOR_HAR_IKKE_SENDT_KRAV,
    FEIL_ANTALL_PLASSER,
    FEIL_KOSTNADSSTED,
    FEIL_PERIODE,
    FEIL_BELOP,
    FEIL_ANNET,
}
