package no.nav.mulighetsrommet.api.utbetaling.model

enum class UtbetalingBeregningType {
    FRI,
    FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
    FAST_SATS_PER_AVTALT_TILTAKSPLASS_PER_MANED,
    PRIS_PER_MANEDSVERK,
    PRIS_PER_UKESVERK,
    PRIS_PER_HELE_UKESVERK,
    PRIS_PER_TIME_OPPFOLGING,
    ;

    companion object {
        fun from(beregning: UtbetalingBeregning): UtbetalingBeregningType = when (beregning) {
            is UtbetalingBeregningFri -> FRI
            is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed -> FAST_SATS_PER_TILTAKSPLASS_PER_MANED
            is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed -> FAST_SATS_PER_AVTALT_TILTAKSPLASS_PER_MANED
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed -> PRIS_PER_MANEDSVERK
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke -> PRIS_PER_UKESVERK
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke -> PRIS_PER_HELE_UKESVERK
            is UtbetalingBeregningAvtaltPrisPerTimeOppfolging -> PRIS_PER_TIME_OPPFOLGING
        }
    }
}
