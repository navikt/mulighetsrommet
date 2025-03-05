package no.nav.mulighetsrommet.api.utbetaling.model

enum class AdminUtbetalingStatus {
    UTBETALT,
    VENTER_PA_ARRANGOR,
    BEHANDLES_AV_NAV,
    ;

    companion object {
        fun fromUtbetaling(
            utbetaling: UtbetalingDto,
            delutbetalinger: List<DelutbetalingDto>,
        ): AdminUtbetalingStatus {
            return if (delutbetalinger.isNotEmpty() && delutbetalinger.all { it is DelutbetalingDto.DelutbetalingUtbetalt }) {
                UTBETALT
            } else if (utbetaling.innsender != null) {
                BEHANDLES_AV_NAV
            } else {
                VENTER_PA_ARRANGOR
            }
        }
    }
}
