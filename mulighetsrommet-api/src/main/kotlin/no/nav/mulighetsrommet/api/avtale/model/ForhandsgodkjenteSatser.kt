package no.nav.mulighetsrommet.api.avtale.model

import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate

object ForhandsgodkjenteSatser {

    fun satser(tiltakskode: Tiltakskode): List<AvtaltSats> {
        return when (tiltakskode) {
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> AFT.satser
            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> VTA.satser
            else -> emptyList()
        }
    }

    fun findSats(tiltakskode: Tiltakskode, periodeStart: LocalDate): Int? {
        val satser = satser(tiltakskode)
        return satser.firstOrNull { periodeStart in it.periode }?.sats
    }

    fun findSats(tiltakskode: Tiltakskode, periode: Periode): Int? {
        val satser = satser(tiltakskode)
        return satser.firstOrNull { periode in it.periode }?.sats
    }

    object VTA {
        val satser: List<AvtaltSats> = listOf(
            AvtaltSats(
                periode = Periode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                ),
                sats = 16_848,
            ),
        )
    }

    object AFT {
        val satser: List<AvtaltSats> = listOf(
            AvtaltSats(
                periode = Periode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                ),
                sats = 20_975,
            ),
        )
    }
}
