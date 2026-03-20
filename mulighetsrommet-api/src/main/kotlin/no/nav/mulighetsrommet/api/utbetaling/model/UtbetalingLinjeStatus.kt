package no.nav.mulighetsrommet.api.utbetaling.model

enum class UtbetalingLinjeStatus(val beskrivelse: String) {
    TIL_ATTESTERING("Til attestering"),
    GODKJENT("Godkjent"),
    RETURNERT("Returnert"),
    UTBETALT("Utbetalt"),
    OVERFORT_TIL_UTBETALING("Overført til utbetaling"),
}
