package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.amo.OpplaringKategorisering
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.InnholdElement
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.model.Tiltakskode

@Serializable
data class AmoKategoriseringDto(
    val kurstype: Kurstype.Kode? = null,
    val bransje: Bransje.Kode? = null,
    val forerkort: List<ForerkortKlasse.Kode>? = null,
    val innholdElementer: List<InnholdElement.Kode>? = null,
    val norskprove: Boolean? = null,
    val sertifiseringer: List<Sertifisering>? = null,
)

fun OpplaringKategorisering.toAmoKategoriseringDto(tiltakskode: Tiltakskode): AmoKategoriseringDto? {
    val innholdsElementer = { innholdElementer.map { it.kode } }
    return when (tiltakskode) {
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ->
            when (kurstype?.kode) {
                Kurstype.Kode.BRANSJE_OG_YRKESRETTET ->
                    AmoKategoriseringDto(
                        kurstype = kurstype.kode,
                        bransje = bransje?.kode,
                        sertifiseringer = sertifiseringer.toList(),
                        forerkort = forerkort.map { it.kode },
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE ->
                    AmoKategoriseringDto(
                        kurstype = kurstype.kode,
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER ->
                    AmoKategoriseringDto(
                        kurstype = kurstype.kode,
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.NORSKOPPLAERING ->
                    AmoKategoriseringDto(
                        kurstype = kurstype.kode,
                        norskprove = norskprove,
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.STUDIESPESIALISERING ->
                    AmoKategoriseringDto(
                        kurstype = kurstype.kode,
                    )

                else -> null
            }

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
            require(kurstype?.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET)
            AmoKategoriseringDto(
                kurstype = kurstype.kode,
                bransje = bransje?.kode,
                sertifiseringer = sertifiseringer.toList(),
                forerkort = forerkort.map { it.kode },
                innholdElementer = innholdsElementer(),
            )
        }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> when (kurstype?.kode) {
            Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> AmoKategoriseringDto(
                kurstype = kurstype.kode,
                innholdElementer = innholdsElementer(),
            )

            Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER -> AmoKategoriseringDto(
                kurstype = kurstype.kode,
                innholdElementer = innholdsElementer(),
            )

            Kurstype.Kode.NORSKOPPLAERING -> AmoKategoriseringDto(
                kurstype = kurstype.kode,
                norskprove = norskprove,
                innholdElementer = innholdsElementer(),
            )

            Kurstype.Kode.STUDIESPESIALISERING,
            Kurstype.Kode.BRANSJE_OG_YRKESRETTET,
            -> throw IllegalStateException("amoKategorisering har feil verdier")

            else -> null
        }

        Tiltakskode.STUDIESPESIALISERING -> AmoKategoriseringDto(kurstype = kurstype?.kode)

        else -> null
    }
}
