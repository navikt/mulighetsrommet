package no.nav.mulighetsrommet.admin.opplaring

import no.nav.mulighetsrommet.api.domain.opplaring.Bransje
import no.nav.mulighetsrommet.api.domain.opplaring.ForerkortKlasse
import no.nav.mulighetsrommet.api.domain.opplaring.InnholdElement
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype

interface OpplaringKategoriseringQueryHandler {
    fun getKurstyper(filter: Set<Kurstype.Kode> = emptySet()): Set<Kurstype>

    fun getBransjer(): Set<Bransje>

    fun getForerkortKlasser(): Set<ForerkortKlasse>

    fun getInnholdElementer(): Set<InnholdElement>

    fun getUtdanningslop(): List<UtdanningslopDetaljer>
}
