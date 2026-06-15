package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.Tilskuddstype

@Serializable
data class UtbetalingBeregningAvtaltPrisPerTimeOppfolging(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    override fun deltakelsePerioder(): Set<DeltakelsePeriode> {
        return input.deltakelser().map {
            DeltakelsePeriode(it.deltakelseId, it.periode())
        }.toSet()
    }

    @Serializable
    data class Input(
        val satser: Set<SatsPeriode>,
        val pris: ValutaBelop,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelsePeriode>,
    ) : UtbetalingBeregningInput() {
        override fun deltakelser() = deltakelser
    }

    @Serializable
    data class Output(override val pris: ValutaBelop) : UtbetalingBeregningOutput() {
        override fun deltakelser() = emptySet<UtbetalingBeregningOutputDeltakelse>()
    }

    companion object {
        fun from(
            satser: Set<SatsPeriode>,
            stengt: Set<StengtPeriode>,
            deltakelser: Set<DeltakelsePeriode>,
            pris: ValutaBelop,
        ): UtbetalingBeregningAvtaltPrisPerTimeOppfolging {
            return UtbetalingBeregningAvtaltPrisPerTimeOppfolging(
                input = Input(satser, pris, stengt, deltakelser),
                output = Output(pris),
            )
        }
    }
}

object PrisPerTimeOppfolgingBeregning :
    SystemgenerertPrismodell.FraDeltakelserOgInnsendtBelop<UtbetalingBeregningAvtaltPrisPerTimeOppfolging> {

    override val type = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
    override val tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD

    override fun beregn(
        gjennomforing: GjennomforingAvtale,
        periode: Periode,
        deltakere: List<Deltaker>,
        pris: ValutaBelop,
    ): UtbetalingBeregningAvtaltPrisPerTimeOppfolging {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengt = UtbetalingInputHelper.resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = UtbetalingInputHelper.resolveDeltakelsePerioder(deltakere, periode)
        return UtbetalingBeregningAvtaltPrisPerTimeOppfolging.from(satser, stengt, deltakelser, pris)
    }
}
