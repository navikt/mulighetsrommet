package no.nav.mulighetsrommet.api.okonomi

import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
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
    }
}
