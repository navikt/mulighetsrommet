package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.InnholdElement
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.janzz.Sertifisering

@Serializable
data class AmoKategoriseringRequest(
    val kurstype: Kurstype.Kode? = null,
    val bransje: Bransje.Kode? = null,
    val forerkort: List<ForerkortKlasse.Kode>? = null,
    val innholdElementer: List<InnholdElement.Kode>? = null,
    val norskprove: Boolean? = null,
    val sertifiseringer: List<Sertifisering>? = null,
)
