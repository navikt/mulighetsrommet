package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop

@Serializable
@SerialName("FAST_SATS_PER_TILTAKSPLASS_PER_MANED")
data class TilsagnBeregningFastSatsPerBenyttetPlassPerManed(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("FAST_SATS_PER_TILTAKSPLASS_PER_MANED")
    data class Input(
        val periode: Periode,
        val sats: ValutaBelop,
        val antallPlasser: Int,
        val stengt: Set<StengtPeriode>,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("FAST_SATS_PER_TILTAKSPLASS_PER_MANED")
    data class Output(
        override val pris: ValutaBelop,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningFastSatsPerBenyttetPlassPerManed {
            val aktivePerioder = input.periode.subtractPeriods(input.stengt.map { it.periode })

            val totalMonths = aktivePerioder
                .map { UtbetalingBeregningHelpers.calculateMonthsInPeriode(it) }
                .sumOf { it }

            val belop = UtbetalingBeregningHelpers.multiplyBySatsAndPlasser(
                totalMonths,
                input.sats,
                input.antallPlasser,
            )

            return TilsagnBeregningFastSatsPerBenyttetPlassPerManed(input, Output(pris = belop))
        }
    }
}
