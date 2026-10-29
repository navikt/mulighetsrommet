package no.nav.mulighetsrommet.api.tilskuddbehandling.db

enum class TilskuddBehandlingStatus(val beskrivelse: String) {
    TIL_GODKJENNING("Til godkjenning"),
    FERDIG_BEHANDLET("Ferdig behandlet"),
    RETURNERT("Returnert"),
}
