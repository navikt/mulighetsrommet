package no.nav.mulighetsrommet.api.utbetaling.model

enum class UtbetalingLinjeStatus(val beskrivelse: String) {
    TIL_GODKJENNING("Til godkjenning"),
    GODKJENT("Godkjent"),
    RETURNERT("Returnert"),
    UTBETALT("Utbetalt"),
    OVERFORT_TIL_UTBETALING("Overført til utbetaling"),
    AVBRUTT("Avbrutt"),
}
