package no.nav.mulighetsrommet.api.okonomi.prismodell

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDeltakelsePerioder
import no.nav.mulighetsrommet.api.okonomi.models.RefusjonskravDeltakelseManedsverk
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.lang.Math.addExact
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.streams.asSequence

object Prismodell {
    object AFT {
        val satser: Map<LocalDate, Int> = mapOf(
            LocalDate.of(2024, 1, 1) to 20205,
            LocalDate.of(2023, 1, 1) to 19500,
        )

        fun findSats(periodeStart: LocalDate): Int {
            val sats = satser
                .filter { !it.key.isAfter(periodeStart) }
                .maxByOrNull { it.key }
            return requireNotNull(sats?.value) {
                "sats mangler for periode med start $periodeStart"
            }
        }

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
            periodeStart: LocalDateTime,
            periodeSlutt: LocalDateTime,
            sats: Int,
            deltakelser: Set<RefusjonskravDeltakelsePerioder>,
        ): RefusjonKravBeregningAft {
            val totalDuration = Duration.between(periodeStart, periodeSlutt).toSeconds().toBigDecimal()

            val manedsverk = deltakelser
                .map { deltkelse ->
                    val perioder = deltkelse.perioder.map { periode ->
                        val start = maxOf(periodeStart, periode.start)
                        val slutt = minOf(periodeSlutt, periode.slutt)
                        val overlapDuration = Duration.between(start, slutt)
                            .toSeconds()
                            .toBigDecimal()

                        val overlapFraction = overlapDuration.divide(totalDuration, 2, RoundingMode.HALF_UP)

                        val stillingsprosent = if (periode.stillingsprosent < 50) {
                            BigDecimal(50)
                        } else {
                            BigDecimal(100)
                        }

                        overlapFraction
                            .multiply(stillingsprosent)
                            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                    }

                    RefusjonskravDeltakelseManedsverk(
                        deltakelseId = deltkelse.deltakelseId,
                        manedsverk = perioder.sumOf { it },
                    )
                }
                .toSet()

            // TODO: hvor nøyaktig skal utregning være?
            val belop = manedsverk
                .fold(BigDecimal.ZERO) { sum, deltakelse ->
                    sum.add(deltakelse.manedsverk)
                }
                .multiply(BigDecimal(sats))
                .setScale(2, RoundingMode.HALF_UP)

            return RefusjonKravBeregningAft(
                belop = belop,
                deltakelser = manedsverk,
            )
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
            val sats: Int,
            val deltakere: Set<RefusjonskravDeltakelsePerioder>,
        ) : RefusjonskravBeregning()
    }
}
