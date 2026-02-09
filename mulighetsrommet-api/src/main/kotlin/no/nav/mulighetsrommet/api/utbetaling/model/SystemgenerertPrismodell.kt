package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.AvtaleGjennomforing
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingInputHelper
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters.nextOrSame
import java.time.temporal.TemporalAdjusters.previousOrSame

interface SystemgenerertPrismodell<B : UtbetalingBeregning> {
    val type: PrismodellType
    val tilskuddstype: Tilskuddstype

    fun justerPeriodeForBeregning(periode: Periode): Periode = periode

    fun beregn(gjennomforing: AvtaleGjennomforing, deltakere: List<Deltaker>, periode: Periode): B
}

object FastSatsPerTiltaksplassPerManedBeregning :
    SystemgenerertPrismodell<UtbetalingBeregningFastSatsPerTiltaksplassPerManed> {
    override val type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
    override val tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD

    override fun beregn(
        gjennomforing: AvtaleGjennomforing,
        deltakere: List<Deltaker>,
        periode: Periode,
    ): UtbetalingBeregningFastSatsPerTiltaksplassPerManed {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengt = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelserPerioderMedDeltakelsesmengder(deltakere, periode)
        val input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(satser, stengt, deltakelser)

        val manedsverk = deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseManedsverkForDeltakelsesprosent(
                    deltakelse,
                    satser,
                    stengt.map { it.periode },
                )
            }
            .toSet()
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(gjennomforing.prismodell.valuta, manedsverk)
        val output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(belop, manedsverk)

        return UtbetalingBeregningFastSatsPerTiltaksplassPerManed(input, output)
    }
}

object PrisPerManedBeregning : SystemgenerertPrismodell<UtbetalingBeregningPrisPerManedsverk> {
    override val type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK
    override val tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD

    override fun beregn(
        gjennomforing: AvtaleGjennomforing,
        deltakere: List<Deltaker>,
        periode: Periode,
    ): UtbetalingBeregningPrisPerManedsverk {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengt = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(deltakere, periode)
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

object PrisPerHeleUkeBeregning : SystemgenerertPrismodell<UtbetalingBeregningPrisPerHeleUkesverk> {
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
        gjennomforing: AvtaleGjennomforing,
        deltakere: List<Deltaker>,
        periode: Periode,
    ): UtbetalingBeregningPrisPerHeleUkesverk {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengt = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(deltakere, periode)
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

object PrisPerUkeBeregning : SystemgenerertPrismodell<UtbetalingBeregningPrisPerUkesverk> {
    override val type = PrismodellType.AVTALT_PRIS_PER_UKESVERK
    override val tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD

    override fun beregn(
        gjennomforing: AvtaleGjennomforing,
        deltakere: List<Deltaker>,
        periode: Periode,
    ): UtbetalingBeregningPrisPerUkesverk {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengt = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(deltakere, periode)
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

private fun resolveStengtHosArrangor(
    periode: Periode,
    stengtPerioder: List<AvtaleGjennomforing.StengtPeriode>,
): Set<StengtPeriode> {
    return stengtPerioder
        .mapNotNull { stengt ->
            Periode.fromInclusiveDates(stengt.start, stengt.slutt).intersect(periode)?.let {
                StengtPeriode(Periode(it.start, it.slutt), stengt.beskrivelse)
            }
        }
        .toSet()
}

private fun resolveDeltakelserPerioderMedDeltakelsesmengder(
    deltakere: List<Deltaker>,
    periode: Periode,
): Set<DeltakelseDeltakelsesprosentPerioder> {
    return deltakere
        .mapNotNull { deltaker ->
            val (deltakelseId, deltakelsePeriode) = UtbetalingInputHelper.toDeltakelsePeriode(deltaker, periode)
                ?: return@mapNotNull null

            val perioder = deltaker.deltakelsesmengder.windowed(2, partialWindows = true).mapNotNull { window ->
                val mengde = window[0]
                val gyldigTil = window.getOrNull(1)?.gyldigFra ?: deltakelsePeriode.slutt

                Periode.of(mengde.gyldigFra, gyldigTil)?.intersect(periode)?.let { overlappingPeriode ->
                    DeltakelsesprosentPeriode(
                        periode = overlappingPeriode,
                        deltakelsesprosent = mengde.deltakelsesprosent,
                    )
                }
            }
            check(perioder.isNotEmpty()) {
                "Deltaker id=$deltakelseId er relevant for utbetaling, men mangler deltakelsesmengder innenfor perioden=$periode"
            }

            DeltakelseDeltakelsesprosentPerioder(deltakelseId, perioder)
        }
        .toSet()
}

private fun resolveDeltakelsePerioder(
    deltakere: List<Deltaker>,
    periode: Periode,
): Set<DeltakelsePeriode> {
    return UtbetalingInputHelper.resolveDeltakelsePerioder(deltakere, periode)
}
