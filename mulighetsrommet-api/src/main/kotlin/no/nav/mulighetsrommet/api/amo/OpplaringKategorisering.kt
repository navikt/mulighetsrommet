package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringDbo
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.janzz.Sertifisering

@Serializable
data class OpplaringKategorisering(
    val kurstype: Kurstype? = null,
    val bransje: Bransje? = null,
    val forerkort: Set<ForerkortKlasse> = emptySet(),
    val innholdElementer: Set<InnholdElement> = emptySet(),
    val norskprove: Boolean? = null,
    val sertifiseringer: Set<Sertifisering> = emptySet(),
    val utdanningslop: UtdanningslopDto? = null,
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

fun OpplaringKategorisering.toDbo(): OpplaringKategoriseringDbo {
    return OpplaringKategoriseringDbo(
        kurstypeId = kurstype?.id,
        bransjeId = bransje?.id,
        forerkort = forerkort.map { it.id }.toSet(),
        innholdElementer = innholdElementer,
        norskprove = norskprove,
        sertifiseringer = sertifiseringer,
        utdanningslop = utdanningslop?.toDbo(),
    )
}
