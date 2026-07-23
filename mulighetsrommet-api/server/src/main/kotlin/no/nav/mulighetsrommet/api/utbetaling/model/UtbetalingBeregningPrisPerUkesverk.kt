package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.Tilskuddstype

@Serializable
data class UtbetalingBeregningPrisPerUkesverk(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val satser: Set<SatsPeriode>,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelsePeriode>,
    ) : UtbetalingBeregningInput() {
        override fun deltakelser() = deltakelser
    }

    @Serializable
    data class Output(
        override val pris: ValutaBelop,
        val deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    ) : UtbetalingBeregningOutput() {
        override fun deltakelser() = deltakelser
    }
}

object PrisPerUkeBeregning : SystemgenerertPrismodell.FraDeltakelser<UtbetalingBeregningPrisPerUkesverk> {
    override val type = PrismodellType.AVTALT_PRIS_PER_UKESVERK
    override val tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD

    override fun beregn(
        gjennomforing: GjennomforingAvtale,
        periode: Periode,
        deltakere: List<Deltaker>,
    ): UtbetalingBeregningPrisPerUkesverk {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengt = UtbetalingInputHelper.resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = UtbetalingInputHelper.resolveDeltakelsePerioder(deltakere, periode)
        val input = UtbetalingBeregningPrisPerUkesverk.Input(satser, stengt, deltakelser)

        val ukesverk = deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseUkesverk(
                    deltakelse,
                    satser,
                    stengt.map { it.periode },
                )
            }
            .toSet()
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(gjennomforing.prismodell.valuta, ukesverk)
        val output = UtbetalingBeregningPrisPerUkesverk.Output(belop, ukesverk)

        return UtbetalingBeregningPrisPerUkesverk(input, output)
    }
}
