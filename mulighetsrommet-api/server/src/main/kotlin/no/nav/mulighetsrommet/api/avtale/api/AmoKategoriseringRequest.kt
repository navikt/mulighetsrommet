package no.nav.mulighetsrommet.api.avtale.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.opplaring.Bransje
import no.nav.mulighetsrommet.api.domain.opplaring.ForerkortKlasse
import no.nav.mulighetsrommet.api.domain.opplaring.InnholdElement
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import no.nav.mulighetsrommet.api.domain.opplaring.Sertifisering

@Serializable
data class AmoKategoriseringRequest(
    val kurstype: Kurstype.Kode? = null,
    val bransje: Bransje.Kode? = null,
    val forerkort: List<ForerkortKlasse.Kode>? = null,
    val innholdElementer: List<InnholdElement.Kode>? = null,
    val norskprove: Boolean? = null,
    val sertifiseringer: List<Sertifisering>? = null,
)
