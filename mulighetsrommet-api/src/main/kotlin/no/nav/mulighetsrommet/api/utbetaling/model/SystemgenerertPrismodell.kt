package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingInputHelper
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

data class UtbetalingGenereringContext(
    val prismodellType: PrismodellType,
    val tilskuddstype: Tilskuddstype,
    val periode: Periode,
)

interface SystemgenerertPrismodell<B : UtbetalingBeregning> {
    val prismodellType: PrismodellType

    fun genereringContext(periode: Periode): UtbetalingGenereringContext

    fun calculate(gjennomforing: GjennomforingGruppetiltak, deltakere: List<Deltaker>, periode: Periode): B
}

object FastSatsPerTiltaksplassPerManedBeregning :
    SystemgenerertPrismodell<UtbetalingBeregningFastSatsPerTiltaksplassPerManed> {
    override val prismodellType = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK

    override fun genereringContext(periode: Periode) = UtbetalingGenereringContext(
        prismodellType = prismodellType,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        periode = periode,
    )

    override fun calculate(
        gjennomforing: GjennomforingGruppetiltak,
        deltakere: List<Deltaker>,
        periode: Periode,
    ): UtbetalingBeregningFastSatsPerTiltaksplassPerManed {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelserPerioderMedDeltakelsesmengder(deltakere, periode)
        val input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(satser, stengtHosArrangor, deltakelser)

        val manedsverk = input.deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseManedsverkForDeltakelsesprosent(
                    deltakelse,
                    input.satser,
                    stengtHosArrangor.map { it.periode },
                )
            }
            .toSet()
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(manedsverk)
        val output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(belop, manedsverk)

        return UtbetalingBeregningFastSatsPerTiltaksplassPerManed(input, output)
    }
}

object PrisPerManedBeregning : SystemgenerertPrismodell<UtbetalingBeregningPrisPerManedsverk> {
    override val prismodellType = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK

    override fun genereringContext(periode: Periode) = UtbetalingGenereringContext(
        prismodellType = prismodellType,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        periode = periode,
    )

    override fun calculate(
        gjennomforing: GjennomforingGruppetiltak,
        deltakere: List<Deltaker>,
        periode: Periode,
    ): UtbetalingBeregningPrisPerManedsverk {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(deltakere, periode)
        val input = UtbetalingBeregningPrisPerManedsverk.Input(satser, stengtHosArrangor, deltakelser)

        val manedsverk = input.deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseManedsverk(
                    deltakelse,
                    input.satser,
                    stengtHosArrangor.map { it.periode },
                )
            }
            .toSet()
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(manedsverk)
        val output = UtbetalingBeregningPrisPerManedsverk.Output(belop, manedsverk)

        return UtbetalingBeregningPrisPerManedsverk(input, output)
    }
}

object PrisPerHeleUkeBeregning : SystemgenerertPrismodell<UtbetalingBeregningPrisPerHeleUkesverk> {
    override val prismodellType = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK

    override fun genereringContext(periode: Periode) = UtbetalingGenereringContext(
        prismodellType = prismodellType,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        periode = heleUkerPeriode(periode),
    )

    override fun calculate(
        gjennomforing: GjennomforingGruppetiltak,
        deltakere: List<Deltaker>,
        periode: Periode,
    ): UtbetalingBeregningPrisPerHeleUkesverk {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(deltakere, periode)
        val input = UtbetalingBeregningPrisPerHeleUkesverk.Input(satser, stengtHosArrangor, deltakelser)

        val ukesverk = input.deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseHeleUkesverk(
                    deltakelse,
                    input.satser,
                    stengtHosArrangor.map { it.periode },
                )
            }
            .toSet()
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(ukesverk)
        val output = UtbetalingBeregningPrisPerHeleUkesverk.Output(belop, ukesverk)

        return UtbetalingBeregningPrisPerHeleUkesverk(input, output)
    }
}

object PrisPerUkeBeregning : SystemgenerertPrismodell<UtbetalingBeregningPrisPerUkesverk> {
    override val prismodellType = PrismodellType.AVTALT_PRIS_PER_UKESVERK

    override fun genereringContext(periode: Periode) = UtbetalingGenereringContext(
        prismodellType = prismodellType,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        periode = periode,
    )

    override fun calculate(
        gjennomforing: GjennomforingGruppetiltak,
        deltakere: List<Deltaker>,
        periode: Periode,
    ): UtbetalingBeregningPrisPerUkesverk {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(deltakere, periode)
        val input = UtbetalingBeregningPrisPerUkesverk.Input(satser, stengtHosArrangor, deltakelser)

        val ukesverk = input.deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseUkesverk(
                    deltakelse,
                    input.satser,
                    stengtHosArrangor.map { it.periode },
                )
            }
            .toSet()
        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(ukesverk)
        val output = UtbetalingBeregningPrisPerUkesverk.Output(belop, ukesverk)

        return UtbetalingBeregningPrisPerUkesverk(input, output)
    }
}

private fun resolveStengtHosArrangor(
    periode: Periode,
    stengtPerioder: List<GjennomforingGruppetiltak.StengtPeriode>,
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

internal fun heleUkerPeriode(periode: Periode): Periode {
    val newStart = if (periode.start.dayOfWeek <= DayOfWeek.WEDNESDAY) {
        periode.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    } else {
        periode.start.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
    }
    val newSlutt = if (periode.slutt.dayOfWeek >= DayOfWeek.THURSDAY) {
        periode.slutt.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
    } else {
        periode.slutt.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    return Periode(newStart, newSlutt)
}
