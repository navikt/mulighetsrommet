package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.amo.AmoKategorisering
import no.nav.mulighetsrommet.api.amo.AmoKurstype
import no.nav.mulighetsrommet.api.amo.OpplaringKategorisering
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.model.Tiltakskode

@Serializable
data class AmoKategoriseringDto(
    val kurstype: AmoKurstype? = null,
    val bransje: AmoKategorisering.BransjeOgYrkesrettet.Bransje? = null,
    val innholdElementer: List<AmoKategorisering.InnholdElement>? = null,
    val sertifiseringer: List<Sertifisering>? = null,
    val forerkort: List<AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse>? = null,
    val norskprove: Boolean? = null,
)

fun OpplaringKategorisering.toAmoKategoriseringDto(tiltakskode: Tiltakskode): AmoKategoriseringDto? {
    val innholdsElementer = { innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.kode.name) } }
    return when (tiltakskode) {
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ->
            when (kurstype?.kode) {
                Kurstype.Kode.BRANSJE_OG_YRKESRETTET ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET,
                        bransje = bransje?.let { AmoKategorisering.BransjeOgYrkesrettet.Bransje.valueOf(it.kode.name) },
                        sertifiseringer = sertifiseringer.toList(),
                        forerkort = forerkort.map {
                            AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse.valueOf(it.kode.name)
                        },
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.NORSKOPPLAERING ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.NORSKOPPLAERING,
                        norskprove = norskprove,
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.STUDIESPESIALISERING ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.STUDIESPESIALISERING,
                    )

                else -> null
            }

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
            require(kurstype?.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET)
            AmoKategoriseringDto(
                kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET,
                bransje = bransje?.let { AmoKategorisering.BransjeOgYrkesrettet.Bransje.valueOf(it.kode.name) },
                sertifiseringer = sertifiseringer.toList(),
                forerkort = forerkort.map {
                    AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse.valueOf(it.kode.name)
                },
                innholdElementer = innholdsElementer(),
            )
        }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> when (kurstype?.kode) {
            Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> AmoKategoriseringDto(
                kurstype = AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                innholdElementer = innholdsElementer(),
            )

            Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER -> AmoKategoriseringDto(
                kurstype = AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
                innholdElementer = innholdsElementer(),
            )

            Kurstype.Kode.NORSKOPPLAERING -> AmoKategoriseringDto(
                kurstype = AmoKurstype.NORSKOPPLAERING,
                norskprove = norskprove,
                innholdElementer = innholdsElementer(),
            )

            Kurstype.Kode.STUDIESPESIALISERING,
            Kurstype.Kode.BRANSJE_OG_YRKESRETTET,
            -> throw IllegalStateException("amoKategorisering har feil verdier")

            else -> null
        }

        Tiltakskode.STUDIESPESIALISERING -> AmoKategoriseringDto(kurstype = AmoKurstype.STUDIESPESIALISERING)

        else -> null
    }
}
