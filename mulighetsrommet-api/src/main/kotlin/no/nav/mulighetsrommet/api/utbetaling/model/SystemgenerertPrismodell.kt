package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingInputHelper
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class UtbetalingGenereringContext(
    val prismodellType: PrismodellType,
    val tilskuddstype: Tilskuddstype,
    val periode: Periode,
)

interface SystemgenerertPrismodell<
    B : UtbetalingBeregning,
    I : UtbetalingBeregningInput,
    > {

    fun genereringContext(periode: Periode): UtbetalingGenereringContext

    fun resolveInput(gjennomforing: GjennomforingGruppetiltak, periode: Periode): I

    fun beregn(input: I): B

    fun calculate(gjennomforing: GjennomforingGruppetiltak, periode: Periode): B {
        val input = resolveInput(gjennomforing, periode)
        return beregn(input)
    }
}

class FastSatsPerTiltaksplassPerManedBeregning(
    private val db: ApiDatabase,
) : SystemgenerertPrismodell<
    UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
    UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input,
    > {
    override fun genereringContext(periode: Periode) = UtbetalingGenereringContext(
        prismodellType = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        periode = periode,
    )

    override fun resolveInput(
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input = db.session {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelserPerioderMedDeltakelsesmengder(gjennomforing.id, periode)
        return UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
            satser = satser,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    override fun beregn(
        input: UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input,
    ): UtbetalingBeregningFastSatsPerTiltaksplassPerManed {
        val stengtHosArrangor = input.stengt.map { it.periode }

        val manedsverk = input.deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseManedsverkForDeltakelsesprosent(
                    deltakelse,
                    input.satser,
                    stengtHosArrangor,
                )
            }
            .toSet()

        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(manedsverk)

        return UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
            input,
            UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(belop, manedsverk),
        )
    }
}

class PrisPerManedBeregning(
    private val db: ApiDatabase,
) : SystemgenerertPrismodell<
    UtbetalingBeregningPrisPerManedsverk,
    UtbetalingBeregningPrisPerManedsverk.Input,
    > {
    override fun genereringContext(periode: Periode) = UtbetalingGenereringContext(
        prismodellType = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        periode = periode,
    )

    override fun resolveInput(
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingBeregningPrisPerManedsverk.Input = db.session {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(gjennomforing.id, periode)
        return UtbetalingBeregningPrisPerManedsverk.Input(
            satser = satser,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    override fun beregn(
        input: UtbetalingBeregningPrisPerManedsverk.Input,
    ): UtbetalingBeregningPrisPerManedsverk {
        val stengtHosArrangor = input.stengt.map { it.periode }

        val manedsverk = input.deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseManedsverk(
                    deltakelse,
                    input.satser,
                    stengtHosArrangor,
                )
            }
            .toSet()

        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(manedsverk)

        return UtbetalingBeregningPrisPerManedsverk(
            input,
            UtbetalingBeregningPrisPerManedsverk.Output(belop, manedsverk),
        )
    }
}

class PrisPerHeleUkeBeregning(
    private val db: ApiDatabase,
) : SystemgenerertPrismodell<
    UtbetalingBeregningPrisPerHeleUkesverk,
    UtbetalingBeregningPrisPerHeleUkesverk.Input,
    > {
    override fun genereringContext(periode: Periode) = UtbetalingGenereringContext(
        prismodellType = PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        periode = heleUkerPeriode(periode),
    )

    override fun resolveInput(
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingBeregningPrisPerHeleUkesverk.Input = db.session {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(gjennomforing.id, periode)
        return UtbetalingBeregningPrisPerHeleUkesverk.Input(
            satser = satser,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    override fun beregn(
        input: UtbetalingBeregningPrisPerHeleUkesverk.Input,
    ): UtbetalingBeregningPrisPerHeleUkesverk {
        val stengtHosArrangor = input.stengt.map { it.periode }

        val ukesverk = input.deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseHeleUkesverk(
                    deltakelse,
                    input.satser,
                    stengtHosArrangor,
                )
            }
            .toSet()

        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(ukesverk)

        return UtbetalingBeregningPrisPerHeleUkesverk(
            input,
            UtbetalingBeregningPrisPerHeleUkesverk.Output(belop, ukesverk),
        )
    }
}

class PrisPerUkeBeregning(
    private val db: ApiDatabase,
) : SystemgenerertPrismodell<
    UtbetalingBeregningPrisPerUkesverk,
    UtbetalingBeregningPrisPerUkesverk.Input,
    > {
    override fun genereringContext(periode: Periode) = UtbetalingGenereringContext(
        prismodellType = PrismodellType.AVTALT_PRIS_PER_UKESVERK,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        periode = periode,
    )

    override fun resolveInput(
        gjennomforing: GjennomforingGruppetiltak,
        periode: Periode,
    ): UtbetalingBeregningPrisPerUkesverk.Input = db.session {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengtHosArrangor = resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelsePerioder(gjennomforing.id, periode)
        return UtbetalingBeregningPrisPerUkesverk.Input(
            satser = satser,
            stengt = stengtHosArrangor,
            deltakelser = deltakelser,
        )
    }

    override fun beregn(
        input: UtbetalingBeregningPrisPerUkesverk.Input,
    ): UtbetalingBeregningPrisPerUkesverk {
        val stengtHosArrangor = input.stengt.map { it.periode }

        val ukesverk = input.deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseUkesverk(
                    deltakelse,
                    input.satser,
                    stengtHosArrangor,
                )
            }
            .toSet()

        val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(ukesverk)

        return UtbetalingBeregningPrisPerUkesverk(
            input,
            UtbetalingBeregningPrisPerUkesverk.Output(belop, ukesverk),
        )
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

private fun QueryContext.resolveDeltakelserPerioderMedDeltakelsesmengder(
    gjennomforingId: UUID,
    periode: Periode,
): Set<DeltakelseDeltakelsesprosentPerioder> {
    return queries.deltaker.getByGjennomforingId(gjennomforingId)
        .asSequence()
        .mapNotNull { deltaker ->
            UtbetalingInputHelper.toDeltakelsePeriode(deltaker, periode)
        }
        .map { (deltakelseId, deltakelsePeriode) ->
            val deltakelsesmengder = queries.deltaker.getDeltakelsesmengder(deltakelseId)

            val perioder = deltakelsesmengder.mapIndexedNotNull { index, mengde ->
                val gyldigTil = deltakelsesmengder.getOrNull(index + 1)?.gyldigFra ?: deltakelsePeriode.slutt

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

private fun QueryContext.resolveDeltakelsePerioder(
    gjennomforingId: UUID,
    periode: Periode,
): Set<DeltakelsePeriode> {
    val deltakere = queries.deltaker.getByGjennomforingId(gjennomforingId)
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
