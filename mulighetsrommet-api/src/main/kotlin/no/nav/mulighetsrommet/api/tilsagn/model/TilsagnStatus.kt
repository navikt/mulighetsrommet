package no.nav.mulighetsrommet.api.tilsagn.model

enum class TilsagnStatus(val beskrivelse: String) {
    TIL_GODKJENNING("Til godkjenning"),
    GODKJENT("Godkjent"),
    RETURNERT("Returnert"),
    TIL_ANNULLERING("Til annullering"),
    ANNULLERT("Annullert"),
    TIL_OPPGJOR("Til oppgjør"),
    OPPGJORT("Oppgjort"),
}
