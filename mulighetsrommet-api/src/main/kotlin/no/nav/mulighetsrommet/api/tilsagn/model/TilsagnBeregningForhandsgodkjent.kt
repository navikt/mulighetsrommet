package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.lang.Math.addExact
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.streams.asSequence

@Serializable
@SerialName("FORHANDSGODKJENT")
data class TilsagnBeregningForhandsgodkjent(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("FORHANDSGODKJENT")
    data class Input(
        @Serializable(with = LocalDateSerializer::class)
        val periodeStart: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val periodeSlutt: LocalDate,
        val sats: Int,
        val antallPlasser: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("FORHANDSGODKJENT")
    data class Output(
        override val belop: Int,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningForhandsgodkjent {
            val (periodeStart, periodeSlutt, sats, antallPlasser) = input

            val output = periodeStart.datesUntil(periodeSlutt.plusDays(1))
                .asSequence()
                .groupBy { it.month }
                .map { (_, datesInMonth) ->
                    val fractionOfMonth = datesInMonth.size.toBigDecimal()
                        .divide(datesInMonth[0].lengthOfMonth().toBigDecimal(), 2, RoundingMode.HALF_UP)

                    val value = fractionOfMonth
                        .multiply(sats.toBigDecimal())
                        .multiply(antallPlasser.toBigDecimal())
                        .setScale(0, RoundingMode.HALF_EVEN)

                    value.intValueExact()
                }
                .reduce { acc: Int, s: Int -> addExact(acc, s) }
                .let { Output(it) }

            return TilsagnBeregningForhandsgodkjent(input, output)
        }
    }
}
