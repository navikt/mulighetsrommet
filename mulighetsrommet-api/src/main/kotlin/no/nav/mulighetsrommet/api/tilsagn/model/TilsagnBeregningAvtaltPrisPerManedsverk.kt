package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import java.lang.Math.addExact
import java.math.RoundingMode
import kotlin.streams.asSequence

/**
 * Presisjon underveis for å oppnå en god beregning av totalbeløpet.
 */
private const val CALCULATION_PRECISION = 20

@Serializable
@SerialName("AVTALT_PRIS_PER_MANEDSVERK")
data class TilsagnBeregningAvtaltPrisPerManedsverk(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("AVTALT_PRIS_PER_MANEDSVERK")
    data class Input(
        val periode: Periode,
        val sats: Int,
        val antallPlasser: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("AVTALT_PRIS_PER_MANEDSVERK")
    data class Output(
        override val belop: Int,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningAvtaltPrisPerManedsverk {
            val (periode, sats, antallPlasser) = input

            val output = periode.start.datesUntil(periode.slutt)
                .asSequence()
                .groupBy { it.month }
                .map { (_, datesInMonth) ->
                    val fractionOfMonth = datesInMonth.size.toBigDecimal()
                        .divide(
                            datesInMonth[0].lengthOfMonth().toBigDecimal(),
                            CALCULATION_PRECISION,
                            RoundingMode.HALF_UP,
                        )

                    val value = fractionOfMonth
                        .multiply(sats.toBigDecimal())
                        .multiply(antallPlasser.toBigDecimal())
                        .setScale(0, RoundingMode.HALF_EVEN)

                    value.intValueExact()
                }
                .reduce { acc: Int, s: Int -> addExact(acc, s) }
                .let { Output(it) }

            return TilsagnBeregningAvtaltPrisPerManedsverk(input, output)
        }
    }
}
