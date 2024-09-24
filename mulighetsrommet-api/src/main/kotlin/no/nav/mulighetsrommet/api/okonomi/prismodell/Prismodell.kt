package no.nav.mulighetsrommet.api.okonomi.prismodell

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell.RefusjonskravBeregning.AFT.Deltaker
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.lang.Math.addExact
import java.lang.Math.multiplyExact
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.streams.asSequence

object Prismodell {
    object AFT {
        val satser: Map<LocalDate, Int> = mapOf(
            LocalDate.of(2024, 1, 1) to 20205,
            LocalDate.of(2023, 1, 1) to 19500,
        )

        fun findSats(periodeStart: LocalDate): Int? =
            satser
                .filter { !it.key.isAfter(periodeStart) }
                .maxByOrNull { it.key }
                ?.value

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
            require(sats == findSats(periodeStart)) {
                "feil sats"
            }
            require(findSats(periodeStart) == findSats(periodeSlutt)) {
                "periode går over flere satser"
            }
            return periodeStart.datesUntil(periodeSlutt.plusDays(1))
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
        }

        fun beregnRefusjonBelop(
            deltakere: List<Deltaker>,
            sats: Int,
            periodeStart: LocalDate,
        ): Int {
            require(sats == findSats(periodeStart)) {
                "feil sats"
            }
            // TODO: Implement
            return multiplyExact(sats, deltakere.size)
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

    @Serializable
    sealed class RefusjonskravBeregning {
        abstract val belop: Int

        @Serializable
        @SerialName("AFT")
        data class AFT(
            override val belop: Int,
            val deltakere: List<Deltaker>,
            val sats: Int,
            @Serializable(with = LocalDateSerializer::class)
            val periodeStart: LocalDate,
        ) : RefusjonskravBeregning() {
            @Serializable
            data class Deltaker(
                @Serializable(with = LocalDateSerializer::class)
                val startDato: LocalDate,
                @Serializable(with = LocalDateSerializer::class)
                val sluttDato: LocalDate,
                val prosentPerioder: List<ProsentPeriode>,
            ) {
                @Serializable
                data class ProsentPeriode(
                    @Serializable(with = LocalDateSerializer::class)
                    val startDato: LocalDate,
                    @Serializable(with = LocalDateSerializer::class)
                    val sluttDato: LocalDate,
                    val prosent: Double,
                )
            }
        }
    }
}
