package no.nav.mulighetsrommet.api.arrangorflate.model

import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType

enum class ArrangorflateUtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    BEHANDLES_AV_NAV,
    UTBETALT,
    UBEHANDLET_FORSLAG,
    OVERFORT_TIL_UTBETALING,
    DELVIS_UTBETALT,
    AVBRUTT,
    ;

    companion object {
        fun fromUtbetaling(status: UtbetalingStatusType, blokkeringer: Set<Utbetaling.Blokkering>): ArrangorflateUtbetalingStatus = when (status) {
            UtbetalingStatusType.GENERERT -> if (blokkeringer.contains(Utbetaling.Blokkering.UBEHANDLET_FORSLAG)) {
                UBEHANDLET_FORSLAG
            } else {
                // TODO: Håndter Utbetaling.Blokkering.MANGLER_TILSAGN
                KLAR_FOR_GODKJENNING
            }

            UtbetalingStatusType.TIL_BEHANDLING,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.TIL_AVBRYTELSE,
            -> BEHANDLES_AV_NAV

            UtbetalingStatusType.FERDIG_BEHANDLET -> OVERFORT_TIL_UTBETALING

            UtbetalingStatusType.DELVIS_UTBETALT -> DELVIS_UTBETALT

            UtbetalingStatusType.UTBETALT -> UTBETALT

            UtbetalingStatusType.AVBRUTT -> AVBRUTT
        }
    }
}
