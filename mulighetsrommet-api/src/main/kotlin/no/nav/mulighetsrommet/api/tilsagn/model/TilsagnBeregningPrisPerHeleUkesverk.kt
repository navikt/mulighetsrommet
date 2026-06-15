package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop

@Serializable
@SerialName("PRIS_PER_HELE_UKESVERK")
data class TilsagnBeregningPrisPerHeleUkesverk(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class Input(
        val periode: Periode,
        val sats: ValutaBelop,
        val antallPlasser: Int,
        val prisbetingelser: String?,
        val stengt: Set<StengtPeriode>,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class Output(
        override val pris: ValutaBelop,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningPrisPerHeleUkesverk {
            val aktivePerioder = input.periode.subtractPeriods(input.stengt.map { it.periode })

            val totalWholeWeeks = aktivePerioder
                .map { UtbetalingBeregningHelpers.calculateWholeWeeksInPeriode(it) }
                .sumOf { it }

            val belop = UtbetalingBeregningHelpers.multiplyBySatsAndPlasser(
                totalWholeWeeks,
                input.sats,
                input.antallPlasser,
            )

            return TilsagnBeregningPrisPerHeleUkesverk(input, Output(belop))
        }
    }
}
