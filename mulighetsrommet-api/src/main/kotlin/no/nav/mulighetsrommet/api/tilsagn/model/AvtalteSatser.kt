package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.mulighetsrommet.api.avtale.mapper.satser
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellDto
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate

object AvtalteSatser {
    fun findSats(avtalteSatser: List<AvtaltSats>, dato: LocalDate): Int? {
        return avtalteSatser.lastOrNull { dato >= it.gjelderFra }?.sats
    }

    fun findSats(avtale: Avtale, periode: Periode): Int? {
        val satser = getAvtalteSatser(avtale)
        val startSats = findSats(satser, periode.start)
        val sluttSats = findSats(satser, periode.slutt)
        if (startSats != sluttSats) {
            return null
        }
        return startSats
    }

    fun getAvtalteSatser(avtale: Avtale): List<AvtaltSats> = when (avtale.prismodell) {
        is PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> getForhandsgodkjenteSatser(avtale.tiltakstype.tiltakskode)
        else -> avtale.prismodell.satser()
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
            ),
        )
    }

    object AFT {
        val satser: List<AvtaltSats> = listOf(
            AvtaltSats(
                gjelderFra = LocalDate.of(2025, 1, 1),
                sats = 20_975,
            ),
        )
    }
}
