package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse
import no.nav.mulighetsrommet.model.AmoKategorisering.BransjeOgYrkesrettet.Sertifisering
import no.nav.mulighetsrommet.model.AmoKurstype
import no.nav.mulighetsrommet.model.Tiltakskode

@Serializable
data class AmoKategoriseringDto(
    val kurstype: AmoKurstype? = null,
    val bransje: AmoKategorisering.BransjeOgYrkesrettet.Bransje? = null,
    val innholdElementer: List<AmoKategorisering.InnholdElement>? = null,
    val sertifiseringer: List<Sertifisering>? = null,
    val forerkort: List<ForerkortKlasse>? = null,
    val norskprove: Boolean? = null,
)

fun AmoKategorisering.toDto(tiltakskode: Tiltakskode): AmoKategoriseringDto? {
    return when (tiltakskode) {
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> when (this) {
            is AmoKategorisering.BransjeOgYrkesrettet -> AmoKategoriseringDto(
                kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET,
                bransje = this.bransje,
                sertifiseringer = this.sertifiseringer,
                forerkort = this.forerkort,
                innholdElementer = this.innholdElementer,
            )

            is AmoKategorisering.ForberedendeOpplaeringForVoksne -> AmoKategoriseringDto(
                kurstype = AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                innholdElementer = this.innholdElementer,
            )

            is AmoKategorisering.GrunnleggendeFerdigheter -> AmoKategoriseringDto(
                kurstype = AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
                innholdElementer = this.innholdElementer,
            )

            is AmoKategorisering.Norskopplaering -> AmoKategoriseringDto(
                kurstype = AmoKurstype.NORSKOPPLAERING,
                norskprove = this.norskprove,
                innholdElementer = this.innholdElementer,
            )

            AmoKategorisering.Studiespesialisering -> AmoKategoriseringDto(
                kurstype = AmoKurstype.STUDIESPESIALISERING,
            )
        }

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
            require(this is AmoKategorisering.BransjeOgYrkesrettet)
            AmoKategoriseringDto(
                kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET,
                bransje = this.bransje,
                sertifiseringer = this.sertifiseringer,
                forerkort = this.forerkort,
                innholdElementer = this.innholdElementer,
            )
        }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> when (this) {
            is AmoKategorisering.ForberedendeOpplaeringForVoksne -> AmoKategoriseringDto(
                kurstype = AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                innholdElementer = this.innholdElementer,
            )

            is AmoKategorisering.GrunnleggendeFerdigheter -> AmoKategoriseringDto(
                kurstype = AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
                innholdElementer = this.innholdElementer,
            )

            is AmoKategorisering.Norskopplaering -> AmoKategoriseringDto(
                kurstype = AmoKurstype.NORSKOPPLAERING,
                norskprove = this.norskprove,
                innholdElementer = this.innholdElementer,
            )

            AmoKategorisering.Studiespesialisering,
            is AmoKategorisering.BransjeOgYrkesrettet,
            -> throw IllegalStateException("amoKategorisering har feil verdier")
        }

        Tiltakskode.STUDIESPESIALISERING -> AmoKategoriseringDto(kurstype = AmoKurstype.STUDIESPESIALISERING)

        else -> null
    }
}
