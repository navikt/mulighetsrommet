package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate

object AvtalteSatser {
    fun findSats(avtale: AvtaleDto, periode: Periode): Int? {
        return getAvtalteSatser(avtale).firstOrNull { periode in it.periode }?.sats
    }

    fun findSats(avtale: AvtaleDto, dato: LocalDate): Int? {
        return getAvtalteSatser(avtale).firstOrNull { dato in it.periode }?.sats
    }

    fun getAvtalteSatser(avtale: AvtaleDto): List<AvtaltSats> = when (avtale.prismodell) {
        Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> getForhandsgodkjenteSatser(avtale.tiltakstype.tiltakskode)

        Prismodell.AVTALT_PRIS_PER_MANEDSVERK, Prismodell.AVTALT_PRIS_PER_UKESVERK -> avtale.satser.map {
            AvtaltSats(
                periode = Periode.fromInclusiveDates(it.periodeStart, it.periodeSlutt),
                sats = it.pris,
            )
        }

        Prismodell.ANNEN_AVTALT_PRIS -> listOf()
    }

    fun getForhandsgodkjenteSatser(tiltakskode: Tiltakskode): List<AvtaltSats> = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> AFT.satser
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> VTA.satser
        else -> emptyList()
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
