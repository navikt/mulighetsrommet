package no.nav.mulighetsrommet.api.tilsagn.model

enum class TilsagnBeregningType {
    ANNEN_AVTALT_PRIS,
    FRI,
    PRIS_PER_MANEDSVERK,
    PRIS_PER_UKESVERK,
    PRIS_PER_HELE_UKESVERK,
    FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
    PRIS_PER_TIME_OPPFOLGING,
    ;

    companion object {
        fun from(beregning: TilsagnBeregning): TilsagnBeregningType = when (beregning) {
            is TilsagnBeregningAnnenAvtaltPris -> ANNEN_AVTALT_PRIS
            is TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed -> PRIS_PER_MANEDSVERK
            is TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke -> PRIS_PER_HELE_UKESVERK
            is TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke -> PRIS_PER_UKESVERK
            is TilsagnBeregningAvtaltPrisPerTimeOppfolgingPerDeltaker -> PRIS_PER_TIME_OPPFOLGING
            is TilsagnBeregningFastSatsPerBenyttetPlassPerManed -> FAST_SATS_PER_TILTAKSPLASS_PER_MANED
            is TilsagnBeregningFri -> FRI
        }
    }
}
