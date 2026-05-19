package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.Tilskuddstype

@Serializable
data class UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val satser: Set<SatsPeriode>,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelseDeltakelsesprosentPerioder>,
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

object FastSatsPerTiltaksplassPerManedBeregning :
    SystemgenerertPrismodell.FraDeltakelser<UtbetalingBeregningFastSatsPerTiltaksplassPerManed> {

    override val type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
    override val tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD

    override fun beregn(
        gjennomforing: GjennomforingAvtale,
        periode: Periode,
        deltakere: List<Deltaker>,
    ): UtbetalingBeregningFastSatsPerTiltaksplassPerManed {
        val satser = UtbetalingInputHelper.resolveAvtalteSatser(gjennomforing, periode)
        val stengt = UtbetalingInputHelper.resolveStengtHosArrangor(periode, gjennomforing.stengt)
        val deltakelser = resolveDeltakelserPerioderMedDeltakelsesmengder(deltakere, periode)
        val input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(satser, stengt, deltakelser)

        val manedsverk = deltakelser
            .map { deltakelse ->
                UtbetalingBeregningHelpers.calculateDeltakelseManedsverkForDeltakelsesprosent(
                    gjennomforing.tiltakstype.tiltakskode,
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
}
