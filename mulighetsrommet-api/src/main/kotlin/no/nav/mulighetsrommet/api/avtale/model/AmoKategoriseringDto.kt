package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.amo.AmoKategorisering
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.model.Tiltakskode

@Serializable
data class AmoKategoriseringDto(
    val kurstype: Kurstype? = null,
    val bransje: Bransje? = null,
    val innholdElementer: Set<AmoKategorisering.InnholdElement>? = null,
    val sertifiseringer: Set<Sertifisering>? = null,
    val forerkort: Set<ForerkortKlasse>? = null,
    val norskprove: Boolean? = null,
)

fun AmoKategorisering.toDto(tiltakskode: Tiltakskode): AmoKategoriseringDto? {
    return when (tiltakskode) {
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        ->
            AmoKategoriseringDto(
                kurstype = this.kurstype,
                bransje = this.bransje,
                sertifiseringer = this.sertifiseringer,
                forerkort = this.forerkort,
                innholdElementer = this.innholdElementer,
            )

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> AmoKategoriseringDto(
            kurstype = this.kurstype,
            innholdElementer = this.innholdElementer,
            norskprove = this.norskprove,
        )

        else -> null
    }
}
