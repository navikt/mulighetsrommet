package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType

enum class ArrangorflateUtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    BEHANDLES_AV_NAV,
    UTBETALT,
    KREVER_ENDRING,
    OVERFORT_TIL_UTBETALING,
    DELVIS_UTBETALT,
    AVBRUTT,
    ;

    companion object {
        fun fromUtbetaling(
            status: UtbetalingStatusType,
            harAdvarsler: Boolean,
        ): ArrangorflateUtbetalingStatus = when (status) {
            UtbetalingStatusType.GENERERT -> {
                if (harAdvarsler) {
                    KREVER_ENDRING
                } else {
                    KLAR_FOR_GODKJENNING
                }
            }

            UtbetalingStatusType.INNSENDT,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
            -> BEHANDLES_AV_NAV

            UtbetalingStatusType.FERDIG_BEHANDLET -> OVERFORT_TIL_UTBETALING

            UtbetalingStatusType.DELVIS_UTBETALT -> DELVIS_UTBETALT

            UtbetalingStatusType.UTBETALT -> UTBETALT

            UtbetalingStatusType.AVBRUTT -> AVBRUTT
        }
    }
}
