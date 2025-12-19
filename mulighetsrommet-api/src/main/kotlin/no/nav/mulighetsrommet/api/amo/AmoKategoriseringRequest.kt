package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AmoKurstype

@Serializable
data class AmoKategoriseringRequest(
    val kurstype: AmoKurstype? = null,
    val bransje: AmoKategorisering.BransjeOgYrkesrettet.Bransje? = null,
    val sertifiseringer: List<AmoKategorisering.BransjeOgYrkesrettet.Sertifisering>? = null,
    val forerkort: List<AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse>? = null,
    val innholdElementer: List<AmoKategorisering.InnholdElement>? = null,
    val norskprove: Boolean? = null,
)
