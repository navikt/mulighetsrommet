package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.model.Periode

object AvtalteSatser {
    fun findSats(avtale: AvtaleDto, periode: Periode): Int? {
        return avtale.satser.firstOrNull {
            periode in Periode.fromInclusiveDates(
                it.periodeStart,
                it.periodeSlutt,
            )
        }?.pris
    }
}
