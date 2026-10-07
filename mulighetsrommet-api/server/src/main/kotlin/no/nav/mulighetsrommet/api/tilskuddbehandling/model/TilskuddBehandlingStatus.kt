package no.nav.mulighetsrommet.api.tilskuddbehandling.model

enum class TilskuddBehandlingStatus(val beskrivelse: String) {
    TIL_ATTESTERING("Til godkjenning"),
    FERDIG_BEHANDLET("Ferdig behandlet"),
    RETURNERT("Returnert"),
}
