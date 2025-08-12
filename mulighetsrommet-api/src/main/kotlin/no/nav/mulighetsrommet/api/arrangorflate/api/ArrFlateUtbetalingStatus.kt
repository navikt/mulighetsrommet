package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType

enum class ArrFlateUtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    BEHANDLES_AV_NAV,
    UTBETALT,
    KREVER_ENDRING,
    OVERFORT_TIL_UTBETALING,
    AVBRUTT,
    ;

    companion object {
        fun fromUtbetaling(
            status: UtbetalingStatusType,
            delutbetalinger: List<Delutbetaling>,
            harAdvarsler: Boolean,
        ): ArrFlateUtbetalingStatus = when (status) {
            UtbetalingStatusType.OPPRETTET -> {
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
            UtbetalingStatusType.FERDIG_BEHANDLET -> {
                if (delutbetalinger.all { it.status == DelutbetalingStatus.UTBETALT }) {
                    UTBETALT
                } else {
                    OVERFORT_TIL_UTBETALING
                }
            }
            UtbetalingStatusType.AVBRUTT -> AVBRUTT
        }

        fun toReadableName(status: ArrFlateUtbetalingStatus): String {
            return when (status) {
                KLAR_FOR_GODKJENNING -> "Klar for godkjenning"
                BEHANDLES_AV_NAV -> "Behandles av NAV"
                UTBETALT -> "Utbetalt"
                KREVER_ENDRING -> "Krever endring"
                OVERFORT_TIL_UTBETALING -> "Overført til utbetaling"
                AVBRUTT -> "Avbrutt"
            }
        }
    }
}
