package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk.Output
import no.nav.mulighetsrommet.model.Periode
import java.lang.Math.addExact
import java.math.RoundingMode
import kotlin.streams.asSequence

/**
 * Presisjon underveis for å oppnå en god beregning av totalbeløpet.
 */
private const val CALCULATION_PRECISION = 20

@Serializable
@SerialName("PRIS_PER_MANEDSVERK")
data class TilsagnBeregningPrisPerManedsverk(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class Input(
        val periode: Periode,
        val sats: Int,
        val antallPlasser: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class Output(
        override val belop: Int,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningPrisPerManedsverk {
            val (periode, sats, antallPlasser) = input

            val belop = manedsverk(periode)
                .map {
                    val value = it
                        .multiply(sats.toBigDecimal())
                        .multiply(antallPlasser.toBigDecimal())
                        .setScale(0, RoundingMode.HALF_EVEN)

                    value.intValueExact()
                }
                .reduce { acc: Int, s: Int -> addExact(acc, s) }

            return TilsagnBeregningPrisPerManedsverk(input, Output(belop = belop))
        }

        fun manedsverk(periode: Periode) = periode.start.datesUntil(periode.slutt)
            .asSequence()
            .groupBy { it.month }
            .map { (_, datesInMonth) ->
                datesInMonth.size.toBigDecimal()
                    .divide(
                        datesInMonth[0].lengthOfMonth().toBigDecimal(),
                        CALCULATION_PRECISION,
                        RoundingMode.HALF_UP,
                    )
            }
    }
}
