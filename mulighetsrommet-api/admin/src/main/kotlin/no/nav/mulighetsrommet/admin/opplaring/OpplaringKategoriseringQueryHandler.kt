package no.nav.mulighetsrommet.admin.opplaring

import no.nav.mulighetsrommet.api.domain.opplaring.Bransje
import no.nav.mulighetsrommet.api.domain.opplaring.ForerkortKlasse
import no.nav.mulighetsrommet.api.domain.opplaring.InnholdElement
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.api.domain.opplaring.UtdanningslopDetaljer
import java.util.UUID

interface OpplaringKategoriseringQueryHandler {
    fun get(kategoriseringId: UUID): OpplaringKategoriseringDetaljer?

    fun upsert(kategoriseringId: UUID, kategorisering: OpplaringKategorisering?)

    fun getKurstyper(filter: Set<Kurstype.Kode> = emptySet()): Set<Kurstype>

    fun getBransjer(): Set<Bransje>

    fun getForerkortKlasser(): Set<ForerkortKlasse>

    fun getInnholdElementer(): Set<InnholdElement>

    fun getUtdanningslop(): List<UtdanningslopDetaljer>
}
