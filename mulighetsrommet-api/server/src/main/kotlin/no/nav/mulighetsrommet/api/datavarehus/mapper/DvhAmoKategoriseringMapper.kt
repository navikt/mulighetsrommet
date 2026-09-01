package no.nav.mulighetsrommet.api.datavarehus.mapper

import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.api.contracts.datavarehus.DvhAmoKategorisering
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import no.nav.mulighetsrommet.model.Tiltakskode

fun OpplaringKategoriseringDetaljer.toDvhAmoKategorisering(tiltakskode: Tiltakskode): DvhAmoKategorisering? {
    val kurstype = this.kurstype
    val innholdsElementer = { innholdElementer.map { it.kode } }
    return when (tiltakskode) {
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ->
            when (kurstype?.kode) {
                Kurstype.Kode.BRANSJE_OG_YRKESRETTET ->
                    DvhAmoKategorisering(
                        kurstype = kurstype.kode,
                        bransje = bransje?.kode,
                        sertifiseringer = sertifiseringer.toList(),
                        forerkort = forerkort.map { it.kode },
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE ->
                    DvhAmoKategorisering(
                        kurstype = kurstype.kode,
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER ->
                    DvhAmoKategorisering(
                        kurstype = kurstype.kode,
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.NORSKOPPLAERING ->
                    DvhAmoKategorisering(
                        kurstype = kurstype.kode,
                        norskprove = norskprove,
                        innholdElementer = innholdsElementer(),
                    )

                Kurstype.Kode.STUDIESPESIALISERING ->
                    DvhAmoKategorisering(
                        kurstype = kurstype.kode,
                    )

                else -> null
            }

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
            require(kurstype?.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET)
            DvhAmoKategorisering(
                kurstype = kurstype.kode,
                bransje = bransje?.kode,
                sertifiseringer = sertifiseringer.toList(),
                forerkort = forerkort.map { it.kode },
                innholdElementer = innholdsElementer(),
            )
        }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> when (kurstype?.kode) {
            Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> DvhAmoKategorisering(
                kurstype = kurstype.kode,
                innholdElementer = innholdsElementer(),
            )

            Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER -> DvhAmoKategorisering(
                kurstype = kurstype.kode,
                innholdElementer = innholdsElementer(),
            )

            Kurstype.Kode.NORSKOPPLAERING -> DvhAmoKategorisering(
                kurstype = kurstype.kode,
                norskprove = norskprove,
                innholdElementer = innholdsElementer(),
            )

            Kurstype.Kode.STUDIESPESIALISERING,
            Kurstype.Kode.BRANSJE_OG_YRKESRETTET,
            -> throw IllegalStateException("amoKategorisering har feil verdier")

            else -> null
        }

        Tiltakskode.STUDIESPESIALISERING -> DvhAmoKategorisering(kurstype = kurstype?.kode)

        else -> null
    }
}
