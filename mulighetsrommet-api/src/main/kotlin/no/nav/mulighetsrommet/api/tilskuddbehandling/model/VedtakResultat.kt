package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable

@Serializable
enum class VedtakResultat(val beskrivelse: String) {
    INNVILGELSE("Innvilgelse"),
    AVSLAG("Avslag"),
}

@Serializable
enum class SamletVedtakResultat {
    INNVILGELSE,
    DELVIS_INNVILGELSE,
    AVSLAG,
}
