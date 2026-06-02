package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.janzz.Sertifisering

@Serializable
data class AmoKategoriseringRequest(
    val kurstype: AmoKurstype? = null,
    val bransje: AmoKategorisering.BransjeOgYrkesrettet.Bransje? = null,
    val sertifiseringer: List<Sertifisering>? = null,
    val forerkort: List<AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse>? = null,
    val innholdElementer: List<AmoKategorisering.InnholdElement>? = null,
    val norskprove: Boolean? = null,
)
