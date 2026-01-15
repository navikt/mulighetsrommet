package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.mulighetsrommet.api.avtale.mapper.satser
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.ValutaType
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate

object AvtalteSatser {
    fun findSats(avtalteSatser: List<AvtaltSats>, dato: LocalDate): Int? {
        return avtalteSatser
            .sortedBy { it.gjelderFra }
            .lastOrNull { dato >= it.gjelderFra }?.sats
    }

    fun getAvtalteSatser(tiltakskode: Tiltakskode, prismodell: Prismodell): List<AvtaltSats> = when (prismodell) {
        is Prismodell.ForhandsgodkjentPrisPerManedsverk -> getForhandsgodkjenteSatser(tiltakskode)
        else -> prismodell.satser()
    }

    fun getForhandsgodkjenteSatser(tiltakskode: Tiltakskode): List<AvtaltSats> = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> AFT.satser
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> VTA.satser
        else -> emptyList()
    }

    object VTA {
        val satser: List<AvtaltSats> = listOf(
            AvtaltSats(
                gjelderFra = LocalDate.of(2025, 1, 1),
                sats = 16_848,
                valuta = ValutaType.NOK,
            ),
            AvtaltSats(
                gjelderFra = LocalDate.of(2026, 1, 1),
                sats = 17_455,
                valuta = ValutaType.NOK,
            ),
        )
    }

    object AFT {
        val satser: List<AvtaltSats> = listOf(
            AvtaltSats(
                gjelderFra = LocalDate.of(2025, 1, 1),
                sats = 20_975,
                valuta = ValutaType.NOK,
            ),
            AvtaltSats(
                gjelderFra = LocalDate.of(2026, 1, 1),
                sats = 21_730,
                valuta = ValutaType.NOK,
            ),
        )
    }
}
