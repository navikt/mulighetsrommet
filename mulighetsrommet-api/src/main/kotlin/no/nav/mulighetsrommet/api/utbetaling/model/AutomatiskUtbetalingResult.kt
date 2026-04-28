package no.nav.mulighetsrommet.api.utbetaling.model

enum class AutomatiskUtbetalingResult {
    FEIL_PRISMODELL,
    FEIL_ANTALL_TILSAGN,
    IKKE_NOK_PENGER,
    UTBETALINGLINJER_ALLEREDE_OPPRETTET,
    VALIDERINGSFEIL,
    GODKJENT,
}
