package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType

enum class ArrangorflateUtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    BEHANDLES_AV_NAV,
    UTBETALT,
    KREVER_ENDRING,
    OVERFORT_TIL_UTBETALING,
    DELVIS_UTBETALT,
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
        }

        fun toReadableName(status: ArrangorflateUtbetalingStatus): String {
            return when (status) {
                KLAR_FOR_GODKJENNING -> "Klar for godkjenning"
                BEHANDLES_AV_NAV -> "Behandles av NAV"
                UTBETALT -> "Utbetalt"
                KREVER_ENDRING -> "Krever endring"
                OVERFORT_TIL_UTBETALING -> "Overført til utbetaling"
                DELVIS_UTBETALT -> "Delvis utbetalt"
            }
        }
    }
}
