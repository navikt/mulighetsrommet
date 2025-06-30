package no.nav.mulighetsrommet.api.utbetaling.model

enum class AutomatiskUtbetalingResult {
    FEIL_PRISMODELL,
    FEIL_ANTALL_TILSAGN,
    IKKE_NOK_PENGER,
    DELUTBETALINGER_ALLEREDE_OPPRETTET,
    GODKJENT,
}
