package no.nav.mulighetsrommet.admin.opplaring

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.opplaring.Bransje
import no.nav.mulighetsrommet.api.domain.opplaring.ForerkortKlasse
import no.nav.mulighetsrommet.api.domain.opplaring.InnholdElement
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.opplaring.Sertifisering

/**
 * Read model for opplæringskategorisering, beriket med visningsdata (navn/kode) for
 * kurstype, bransje, førerkort og innholdselementer.
 */
@Serializable
data class OpplaringKategoriseringDetaljer(
    val kurstype: Kurstype? = null,
    val bransje: Bransje? = null,
    val forerkort: Set<ForerkortKlasse> = emptySet(),
    val innholdElementer: Set<InnholdElement> = emptySet(),
    val norskprove: Boolean? = null,
    val sertifiseringer: Set<Sertifisering> = emptySet(),
    val utdanningslop: UtdanningslopDetaljer? = null,
)

fun OpplaringKategoriseringDetaljer.toOpplaringKategorisering(): OpplaringKategorisering = OpplaringKategorisering(
    kurstype = kurstype?.id,
    bransje = bransje?.id,
    forerkort = forerkort.map { it.id }.toSet(),
    innholdElementer = innholdElementer.map { it.id }.toSet(),
    norskprove = norskprove,
    sertifiseringer = sertifiseringer,
    utdanningslop = utdanningslop?.toUtdanningslop(),
)
