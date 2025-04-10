package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
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

    fun findSats(tiltakskode: Tiltakskode, periode: Periode): Int? {
        val satser = satser(tiltakskode)
        return satser.firstOrNull { periode in it.periode }?.belop
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
        )
    }
}
