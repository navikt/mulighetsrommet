package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.amo.AmoKategorisering
import no.nav.mulighetsrommet.api.amo.AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse
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

fun OpplaringKategorisering.toAmoKategoriseringDto(tiltakskode: Tiltakskode): AmoKategoriseringDto? {
    return when (tiltakskode) {
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ->
            when (this.kurstype?.kode) {
                Kurstype.Kode.BRANSJE_OG_YRKESRETTET ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET,
                        bransje = this.bransje?.let { AmoKategorisering.BransjeOgYrkesrettet.Bransje.valueOf(it.kode.toString()) },
                        sertifiseringer = this.sertifiseringer.toList(),
                        forerkort = this.forerkort.map {
                            AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse.valueOf(
                                it.kode.toString(),
                            )
                        },
                        innholdElementer = this.innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.toString()) },
                    )

                Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                        innholdElementer = this.innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.toString()) },
                    )

                Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
                        innholdElementer = this.innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.toString()) },
                    )

                Kurstype.Kode.NORSKOPPLAERING ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.NORSKOPPLAERING,
                        norskprove = this.norskprove,
                        innholdElementer = this.innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.toString()) },
                    )

                Kurstype.Kode.STUDIESPESIALISERING ->
                    AmoKategoriseringDto(
                        kurstype = AmoKurstype.STUDIESPESIALISERING,
                    )

                else -> null
            }

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
            require(this.kurstype?.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET)
            AmoKategoriseringDto(
                kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET,
                bransje = this.bransje?.let { AmoKategorisering.BransjeOgYrkesrettet.Bransje.valueOf(it.kode.toString()) },
                sertifiseringer = this.sertifiseringer.toList(),
                forerkort = this.forerkort.map {
                    AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse.valueOf(
                        it.kode.toString(),
                    )
                },
                innholdElementer = this.innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.toString()) },
            )
        }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> when (this.kurstype?.kode) {
            Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> AmoKategoriseringDto(
                kurstype = AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                innholdElementer = this.innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.toString()) },
            )

            Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER -> AmoKategoriseringDto(
                kurstype = AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
                innholdElementer = this.innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.toString()) },
            )

            Kurstype.Kode.NORSKOPPLAERING -> AmoKategoriseringDto(
                kurstype = AmoKurstype.NORSKOPPLAERING,
                norskprove = this.norskprove,
                innholdElementer = this.innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.toString()) },
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
