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
        TIL_GODKJENNING -> "til godkjenning"
        GODKJENT -> "godkjent"
        RETURNERT -> "returnert"
        TIL_ANNULLERING -> "til annullering"
        ANNULLERT -> "annullert"
        TIL_OPPGJOR -> "til oppgjør"
        OPPGJORT -> "oppgjort"
    }
}
