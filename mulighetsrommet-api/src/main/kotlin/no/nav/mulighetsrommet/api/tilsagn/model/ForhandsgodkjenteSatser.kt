package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.Periode
import java.time.LocalDate

data class ForhandsgodkjentSats(
    val periode: Periode,
    val belop: Int,
)

object ForhandsgodkjenteSatser {

    fun satser(tiltakskode: Tiltakskode): List<ForhandsgodkjentSats> {
        return when (tiltakskode) {
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> AFT.satser
            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> VTA.satser
            else -> emptyList()
        }
    }

    fun findSats(tiltakskode: Tiltakskode, periodeStart: LocalDate): Int? {
        val satser = satser(tiltakskode)
        return satser.firstOrNull { periodeStart in it.periode }?.belop
    }

    object VTA {
        val satser: List<ForhandsgodkjentSats> = listOf(
            ForhandsgodkjentSats(
                periode = Periode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                ),
                belop = 16_848,
            ),
            ForhandsgodkjentSats(
                periode = Periode(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2025, 1, 1),
                ),
                belop = 16_231,
            ),
        )
    }

    object AFT {
        val satser: List<ForhandsgodkjentSats> = listOf(
            ForhandsgodkjentSats(
                periode = Periode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                ),
                belop = 20_975,
            ),
            ForhandsgodkjentSats(
                periode = Periode(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2025, 1, 1),
                ),
                belop = 20_205,
            ),
        )
    }
}
