package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.janzz.Sertifisering

@Serializable
data class AmoKategoriseringRequest(
    val kurstype: Kurstype.Kode? = null,
    val bransje: Bransje.Kode? = null,
    val sertifiseringer: Set<Sertifisering>? = null,
    val forerkort: Set<ForerkortKlasse.Kode>? = null,
    val innholdElementer: Set<AmoKategorisering.InnholdElement>? = null,
    val norskprove: Boolean? = null,
)
