package no.nav.mulighetsrommet.api.tilsagn.model

enum class TilsagnStatus {
    TIL_GODKJENNING,
    GODKJENT,
    RETURNERT,
    TIL_ANNULLERING,
    ANNULLERT,
    TIL_OPPGJOR,
    OPPGJORT,
    ;

    fun navn(): String = when (this) {
        TIL_GODKJENNING -> "Til godkjenning"
        GODKJENT -> "Godkjent"
        RETURNERT -> "Returnert"
        TIL_ANNULLERING -> "Til annullering"
        ANNULLERT -> "Annullert"
        TIL_OPPGJOR -> "Til oppgjør"
        OPPGJORT -> "Oppgjort"
    }
}
