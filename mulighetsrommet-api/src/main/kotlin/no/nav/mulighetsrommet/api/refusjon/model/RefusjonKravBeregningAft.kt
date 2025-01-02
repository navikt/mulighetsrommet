package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

data class RefusjonKravBeregningAft(
    override val input: Input,
    override val output: Output,
) : RefusjonKravBeregning() {

    data class Input(
        override val periode: RefusjonskravPeriode,
        val sats: Int,
        val deltakelser: Set<DeltakelsePerioder>,
    ) : RefusjonKravBeregningInput()

    data class Output(
        override val belop: Int,
        val deltakelser: Set<DeltakelseManedsverk>,
    ) : RefusjonKravBeregningOutput()

    companion object {
        fun beregn(input: Input): RefusjonKravBeregningAft {
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

            val output = Output(
                belop = belop,
                deltakelser = manedsverk,
            )

            return RefusjonKravBeregningAft(input, output)
        }
    }
}

@Serializable
data class DeltakelsePerioder(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val perioder: List<DeltakelsePeriode>,
)

@Serializable
data class DeltakelsePeriode(
    @Serializable(with = LocalDateSerializer::class)
    val start: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val slutt: LocalDate,
    val deltakelsesprosent: Double,
)

@Serializable
data class DeltakelseManedsverk(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val manedsverk: Double,
)
