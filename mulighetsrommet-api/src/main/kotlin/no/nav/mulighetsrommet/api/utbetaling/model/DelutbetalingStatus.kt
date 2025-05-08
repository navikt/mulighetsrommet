package no.nav.mulighetsrommet.api.utbetaling.model

enum class DelutbetalingStatus {
    TIL_GODKJENNING,
    GODKJENT,
    RETURNERT,
    UTBETALT,
    OVERFORT_TIL_UTBETALING,
    BEHANDLES_AV_NAV,
}
