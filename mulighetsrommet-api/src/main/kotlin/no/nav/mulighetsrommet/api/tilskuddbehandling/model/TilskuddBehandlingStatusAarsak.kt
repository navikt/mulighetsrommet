package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable

@Serializable
enum class TilskuddBehandlingStatusAarsak(val beskrivelse: String) {
    FEIL_SAKSOPPLYSNINGER("Feil i saksopplysninger"),
    FEIL_BELOP("Feil beløp"),
    FEIL_VEDTAKSRESULTAT("Feil vedtaksresultat"),
    ANNET("Annet"),
}
