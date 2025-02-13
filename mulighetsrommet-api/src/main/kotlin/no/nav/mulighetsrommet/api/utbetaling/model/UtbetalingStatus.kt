package no.nav.mulighetsrommet.api.utbetaling.model

enum class UtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    INNSENDT_AV_ARRANGOR,
    OPPRETTET_AV_NAV,
    OVERFORT_TIL_UTBETALING,
    UTBETALT,
}
