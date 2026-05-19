package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters.nextOrSame
import java.time.temporal.TemporalAdjusters.previousOrSame

@Serializable
data class UtbetalingBeregningPrisPerHeleUkesverk(
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

object PrisPerHeleUkeBeregning : SystemgenerertPrismodell.FraDeltakelser<UtbetalingBeregningPrisPerHeleUkesverk> {
    override val type = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK
    override val tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD

    override fun justerPeriodeForBeregning(periode: Periode): Periode {
        val newStart = if (periode.start.dayOfWeek <= DayOfWeek.WEDNESDAY) {
            periode.start.with(previousOrSame(DayOfWeek.MONDAY))
        } else {
            periode.start.with(nextOrSame(DayOfWeek.MONDAY))
        }
        val newSlutt = if (periode.slutt.dayOfWeek >= DayOfWeek.THURSDAY) {
            periode.slutt.with(nextOrSame(DayOfWeek.MONDAY))
        } else {
            periode.slutt.with(previousOrSame(DayOfWeek.MONDAY))
        }
        return Periode(newStart, newSlutt)
    }

    override fun beregn(
        gjennomforing: GjennomforingAvtale,
        periode: Periode,
        deltakere: List<Deltaker>,
    ): UtbetalingBeregningPrisPerHeleUkesverk {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengt = UtbetalingInputHelper.resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = UtbetalingInputHelper.resolveDeltakelsePerioder(deltakere, periode)
        val input = UtbetalingBeregningPrisPerHeleUkesverk.Input(satser, stengt, deltakelser)

        val ukesverk = deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseHeleUkesverk(
                    deltakelse,
                    satser,
                    stengt.map { it.periode },
                )
            }
            .toSet()
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(gjennomforing.prismodell.valuta, ukesverk)
        val output = UtbetalingBeregningPrisPerHeleUkesverk.Output(belop, ukesverk)

        return UtbetalingBeregningPrisPerHeleUkesverk(input, output)
    }
}
