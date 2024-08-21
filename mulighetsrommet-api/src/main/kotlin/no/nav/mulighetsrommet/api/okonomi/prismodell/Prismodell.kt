package no.nav.mulighetsrommet.api.okonomi.prismodell

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.lang.Math.addExact
import java.time.LocalDate
import java.time.Month
import kotlin.math.min
import kotlin.math.round
import kotlin.streams.asSequence

object Prismodell {
    object AFT {
        fun beregnTilsagnBelop(
            sats: Int,
            antallPlasser: Int,
            periodeStart: LocalDate,
            periodeSlutt: LocalDate,
        ): Int {
            require(!periodeStart.isAfter(periodeSlutt)) {
                "periodeSlutt kan ikke være før periodeStart"
            }
            require(periodeStart.year == periodeSlutt.year) {
                "perioden må være innen et år"
            }
            return periodeStart.datesUntil(periodeSlutt.plusDays(1))
                .asSequence()
                .groupBy { it.month }
                .map { (month, datesInMonth) ->
                    // Det er sånn Arena regner det ut... Altså at man tar som regel antall dager / 30, men mindre man
                    // er i februar. Dvs at 30 dager i august gir 100 % selv om det er 31 dager i august
                    val fractionOfMonth = if (month == Month.FEBRUARY) {
                        datesInMonth.size.toDouble() / datesInMonth[0].lengthOfMonth()
                    } else {
                        min(datesInMonth.size.toDouble() / 30, 1.0)
                    }

                    val value = round(fractionOfMonth * sats * antallPlasser)
                    if (value > Int.MAX_VALUE) {
                        throw ArithmeticException()
                    }
                    value.toInt()
                }
                .reduce { acc: Int, s: Int -> addExact(acc, s) }
        }
    }

    @Serializable
    sealed class TilsagnBeregning {
        abstract val belop: Int

        @Serializable
        @SerialName("AFT")
        data class AFT(
            override val belop: Int,
            val sats: Int,
            val antallPlasser: Int,
            @Serializable(with = LocalDateSerializer::class)
            val periodeStart: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val periodeSlutt: LocalDate,
        ) : TilsagnBeregning()

        @Serializable
        @SerialName("FRI")
        data class Fri(override val belop: Int) : TilsagnBeregning()
    }
}
