package no.nav.mulighetsrommet.api.arrangorflate.model

import no.nav.mulighetsrommet.api.arrangorflate.RelevanteForslag
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto

enum class ArrFlateUtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    BEHANDLES_AV_NAV,
    UTBETALT,
    VENTER_PA_ENDRING,
    ;

    companion object {
        fun fromUtbetaling(
            utbetaling: UtbetalingDto,
            delutbetalinger: List<DelutbetalingDto>,
            relevanteForslag: List<RelevanteForslag>,
        ): ArrFlateUtbetalingStatus {
            return if (delutbetalinger.isNotEmpty() && delutbetalinger.all { it is DelutbetalingDto.DelutbetalingUtbetalt }) {
                UTBETALT
            } else if (utbetaling.innsender != null) {
                BEHANDLES_AV_NAV
            } else if (relevanteForslag.any { it.antallRelevanteForslag > 0 }) {
                VENTER_PA_ENDRING
            } else {
                KLAR_FOR_GODKJENNING
            }
        }
    }
}
