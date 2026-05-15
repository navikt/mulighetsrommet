package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.Tilskuddstype

@Serializable
data class UtbetalingBeregningPrisPerManedsverk(
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

object PrisPerManedBeregning : SystemgenerertPrismodell<UtbetalingBeregningPrisPerManedsverk> {

    override val type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK
    override val tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD

    override fun beregn(
        gjennomforing: GjennomforingAvtale,
        deltakere: List<Deltaker>,
        periode: Periode,
    ): UtbetalingBeregningPrisPerManedsverk {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengt = UtbetalingInputHelper.resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = UtbetalingInputHelper.resolveDeltakelsePerioder(deltakere, periode)
        val input = UtbetalingBeregningPrisPerManedsverk.Input(satser, stengt, deltakelser)

        val manedsverk = deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseManedsverk(
                    deltakelse,
                    satser,
                    stengt.map { it.periode },
                )
            }
            .toSet()
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(gjennomforing.prismodell.valuta, manedsverk)
        val output = UtbetalingBeregningPrisPerManedsverk.Output(belop, manedsverk)

        return UtbetalingBeregningPrisPerManedsverk(input, output)
    }
}
