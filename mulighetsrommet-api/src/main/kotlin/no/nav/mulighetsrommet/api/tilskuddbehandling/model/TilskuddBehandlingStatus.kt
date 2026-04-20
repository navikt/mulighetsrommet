package no.nav.mulighetsrommet.api.tilskuddbehandling.model

enum class TilskuddBehandlingStatus(val beskrivelse: String) {
    TIL_GODKJENNING("Til godkjenning"),
    GODKJENT("Godkjent"),
    RETURNERT("Returnert"),
}
