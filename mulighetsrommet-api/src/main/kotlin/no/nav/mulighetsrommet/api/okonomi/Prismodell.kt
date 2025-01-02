package no.nav.mulighetsrommet.api.okonomi

import no.nav.mulighetsrommet.api.refusjon.model.DeltakelseManedsverk
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class ForhandsgodkjentSats(
    val periode: RefusjonskravPeriode,
    val belop: Int,
)

object Prismodell {
    object VTA {
        val satser: List<ForhandsgodkjentSats> = listOf(
            ForhandsgodkjentSats(
                periode = RefusjonskravPeriode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                ),
                belop = 16000,
            ),
            ForhandsgodkjentSats(
                periode = RefusjonskravPeriode(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2025, 1, 1),
                ),
                belop = 12000,
            ),
        )
    }

    object AFT {
        val satser: List<ForhandsgodkjentSats> = listOf(
            ForhandsgodkjentSats(
                periode = RefusjonskravPeriode(
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2027, 1, 1),
                ),
                belop = 30000,
            ),
            ForhandsgodkjentSats(
                periode = RefusjonskravPeriode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                ),
                belop = 20705,
            ),
            ForhandsgodkjentSats(
                periode = RefusjonskravPeriode(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2025, 1, 1),
                ),
                belop = 20205,
            ),
            ForhandsgodkjentSats(
                periode = RefusjonskravPeriode(
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2024, 1, 1),
                ),
                belop = 19500,
            ),
        )

        fun findSats(periodeStart: LocalDate): Int? {
            return satser.firstOrNull { periodeStart in it.periode }?.belop
        }

        fun beregnRefusjonBelop(input: RefusjonKravBeregningAft.Input): RefusjonKravBeregningAft.Output {
            val (periode, sats, deltakelser) = input
            val totalDuration = periode.getDurationInDays().toBigDecimal()

            val manedsverk = deltakelser
                .map { deltkelse ->
                    val perioder = deltkelse.perioder.map { deltakelsePeriode ->
                        val start = maxOf(periode.start, deltakelsePeriode.start)
                        val slutt = minOf(periode.slutt, deltakelsePeriode.slutt)
                        val overlapDuration = RefusjonskravPeriode(start, slutt).getDurationInDays().toBigDecimal()

                        val overlapFraction = overlapDuration.divide(totalDuration, 2, RoundingMode.HALF_UP)

                        val deltakelsesprosent = if (deltakelsePeriode.deltakelsesprosent < 50) {
                            BigDecimal(50)
                        } else {
                            BigDecimal(100)
                        }

                        overlapFraction
                            .multiply(deltakelsesprosent)
                            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                    }

                    DeltakelseManedsverk(
                        deltakelseId = deltkelse.deltakelseId,
                        manedsverk = perioder.sumOf { it }.toDouble(),
                    )
                }
                .toSet()

            // TODO: hvor nøyaktig skal utregning være?
            val belop = manedsverk
                .fold(BigDecimal.ZERO) { sum, deltakelse ->
                    sum.add(BigDecimal(deltakelse.manedsverk))
                }
                .multiply(BigDecimal(sats))
                .setScale(2, RoundingMode.HALF_UP)
                .toInt()

            return RefusjonKravBeregningAft.Output(
                belop = belop,
                deltakelser = manedsverk,
            )
        }
    }
}
