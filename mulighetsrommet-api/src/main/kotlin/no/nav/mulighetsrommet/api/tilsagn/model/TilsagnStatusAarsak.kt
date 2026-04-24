package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable

@Serializable
enum class TilsagnStatusAarsak(val beskrivelse: String) {
    FEIL_REGISTRERING("Feilregistrering"),
    TILTAK_SKAL_IKKE_GJENNOMFORES("Tiltak skal ikke gjennomføres"),
    ARRANGOR_HAR_IKKE_SENDT_KRAV("Arrangør har ikke sendt krav"),
    FEIL_ANTALL_PLASSER("Feil antall plasser"),
    FEIL_KOSTNADSSTED("Feil kostnadssted"),
    FEIL_PERIODE("Feil periode"),
    FEIL_BELOP("Feil beløp"),
    ANNET("Annet"),
}
