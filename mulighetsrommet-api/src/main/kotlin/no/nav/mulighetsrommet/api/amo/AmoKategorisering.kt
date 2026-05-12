package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.janzz.Sertifisering

@Serializable
data class AmoKategorisering(
    val kurstype: Kurstype? = null,
    val bransje: Bransje? = null,
    val forerkort: Set<ForerkortKlasse> = emptySet(),
    val innholdElementer: Set<InnholdElement> = emptySet(),
    val norskprove: Boolean? = null,
    val sertifiseringer: Set<Sertifisering> = emptySet(),
) {
    enum class InnholdElement {
        GRUNNLEGGENDE_FERDIGHETER,
        BRANSJERETTET_OPPLARING,
        TEORETISK_OPPLAERING,
        JOBBSOKER_KOMPETANSE,
        PRAKSIS,
        ARBEIDSMARKEDSKUNNSKAP,
        NORSKOPPLAERING,
    }
}
