package no.nav.mulighetsrommet.api.utbetaling.model

enum class UtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    INNSENDT_AV_ARRANGOR,
    INNSENDT_AV_NAV,
    UTBETALT,
    VENTER_PA_ENDRING,
}
