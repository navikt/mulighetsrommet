package no.nav.mulighetsrommet.api.arrangorflate.model

import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType

enum class ArrangorflateUtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    BEHANDLES_AV_NAV,
    UTBETALT,
    BLOKKERT_FOR_INNSENDING,
    OVERFORT_TIL_UTBETALING,
    DELVIS_UTBETALT,
    AVBRUTT_AV_NAV,
    AVBRUTT_AV_ARRANGOR,
    ;

    companion object {
        fun fromUtbetaling(
            status: UtbetalingStatusType,
            blokkeringer: Set<Utbetaling.Blokkering>,
            avbrytelseTotrinnskontroll: TotrinnskontrollDto?,
        ): ArrangorflateUtbetalingStatus = when (status) {
            UtbetalingStatusType.GENERERT -> if (blokkeringer.isNotEmpty()) {
                BLOKKERT_FOR_INNSENDING
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

            UtbetalingStatusType.AVBRUTT -> utledAvbruttStatus(avbrytelseTotrinnskontroll)
        }

        private fun utledAvbruttStatus(avbrytelse: TotrinnskontrollDto?): ArrangorflateUtbetalingStatus = when {
            avbrytelse is TotrinnskontrollDto.Besluttet && avbrytelse.beslutning == TotrinnskontrollDto.Beslutning.GODKJENT -> AVBRUTT_AV_NAV
            else -> AVBRUTT_AV_ARRANGOR
        }
    }
}
