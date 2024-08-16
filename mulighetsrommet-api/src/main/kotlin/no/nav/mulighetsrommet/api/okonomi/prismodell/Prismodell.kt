package no.nav.mulighetsrommet.api.okonomi.prismodell

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.streams.asSequence

object Prismodell {
    object AFT {
        fun beregnTilsagnBelop(
            sats: Int,
            antallPlasser: Int,
            periodeStart: LocalDate,
            periodeSlutt: LocalDate,
        ): Int {
            return periodeStart.datesUntil(periodeSlutt.plusDays(1))
                .asSequence()
                .groupBy { it.month }
                .map { (month, datesInMonth) ->
                    val fractionOfMonth = datesInMonth.size.toDouble() / month.length(datesInMonth[0].isLeapYear).toDouble()

                    ceil(fractionOfMonth * sats * antallPlasser).toInt()
                }
                .sum()
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
