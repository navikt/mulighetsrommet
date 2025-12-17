package no.nav.mulighetsrommet.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

enum class AmoKurstype {
    BRANSJE_OG_YRKESRETTET,
    NORSKOPPLAERING,
    GRUNNLEGGENDE_FERDIGHETER,
    FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
    STUDIESPESIALISERING,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kurstype")
sealed class AmoKategorisering {
    @Serializable
    @SerialName("BRANSJE_OG_YRKESRETTET")
    data class BransjeOgYrkesrettet(
        val bransje: Bransje,
        val sertifiseringer: List<Sertifisering>,
        val forerkort: List<ForerkortKlasse>,
        val innholdElementer: List<InnholdElement>,
    ) : AmoKategorisering() {
        enum class Bransje {
            INGENIOR_OG_IKT_FAG,
            HELSE_PLEIE_OG_OMSORG,
            BARNE_OG_UNGDOMSARBEID,
            KONTORARBEID,
            BUTIKK_OG_SALGSARBEID,
            BYGG_OG_ANLEGG,
            INDUSTRIARBEID,
            REISELIV_SERVERING_OG_TRANSPORT,
            SERVICEYRKER_OG_ANNET_ARBEID,
            ANDRE_BRANSJER,
        }

        @Serializable
        data class Sertifisering(val konseptId: Long, val label: String)

        enum class ForerkortKlasse {
            A,
            A1,
            A2,
            AM,
            AM_147,
            B,
            B_78,
            BE,
            C,
            C1,
            C1E,
            CE,
            D,
            D1,
            D1E,
            DE,
            S,
            T,
        }
    }

    @Serializable
    @SerialName("NORSKOPPLAERING")
    data class Norskopplaering(
        val norskprove: Boolean,
        val innholdElementer: List<InnholdElement>,
    ) : AmoKategorisering()

    @Serializable
    @SerialName("GRUNNLEGGENDE_FERDIGHETER")
    data class GrunnleggendeFerdigheter(
        val innholdElementer: List<InnholdElement>,
    ) : AmoKategorisering()

    @Serializable
    @SerialName("FORBEREDENDE_OPPLAERING_FOR_VOKSNE")
    data object ForberedendeOpplaeringForVoksne : AmoKategorisering()

    @Serializable
    @SerialName("STUDIESPESIALISERING")
    data object Studiespesialisering : AmoKategorisering()

    enum class InnholdElement {
        GRUNNLEGGENDE_FERDIGHETER,
        TEORETISK_OPPLAERING,
        JOBBSOKER_KOMPETANSE,
        PRAKSIS,
        ARBEIDSMARKEDSKUNNSKAP,
        NORSKOPPLAERING,
    }

    companion object {
        fun from(request: AmoKategoriseringRequest): AmoKategorisering {
            requireNotNull(request.kurstype)
            return when (request.kurstype) {
                AmoKurstype.BRANSJE_OG_YRKESRETTET -> BransjeOgYrkesrettet(
                    bransje = requireNotNull(request.bransje),
                    sertifiseringer = request.sertifiseringer ?: emptyList(),
                    innholdElementer = request.innholdElementer ?: emptyList(),
                    forerkort = request.forerkort ?: emptyList(),
                )

                AmoKurstype.NORSKOPPLAERING -> Norskopplaering(
                    norskprove = request.norskprove ?: false,
                    innholdElementer = request.innholdElementer ?: emptyList(),
                )

                AmoKurstype.GRUNNLEGGENDE_FERDIGHETER -> GrunnleggendeFerdigheter(
                    innholdElementer = request.innholdElementer ?: emptyList(),
                )

                AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> ForberedendeOpplaeringForVoksne

                AmoKurstype.STUDIESPESIALISERING -> Studiespesialisering
            }
        }
    }
}

@Serializable
data class AmoKategoriseringRequest(
    val kurstype: AmoKurstype?,
    val bransje: AmoKategorisering.BransjeOgYrkesrettet.Bransje? = null,
    val sertifiseringer: List<AmoKategorisering.BransjeOgYrkesrettet.Sertifisering>? = null,
    val forerkort: List<AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse>? = null,
    val innholdElementer: List<AmoKategorisering.InnholdElement>? = null,
    val norskprove: Boolean? = null,
) {
    companion object {
        fun from(amoKategorisering: AmoKategorisering) = when (amoKategorisering) {
            is AmoKategorisering.BransjeOgYrkesrettet -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET,
                bransje = amoKategorisering.bransje,
                sertifiseringer = amoKategorisering.sertifiseringer,
                forerkort = amoKategorisering.forerkort,
                innholdElementer = amoKategorisering.innholdElementer,
                norskprove = null,
            )

            AmoKategorisering.ForberedendeOpplaeringForVoksne -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                bransje = null,
                sertifiseringer = null,
                forerkort = null,
                innholdElementer = null,
                norskprove = null,
            )

            is AmoKategorisering.GrunnleggendeFerdigheter -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                bransje = null,
                sertifiseringer = null,
                forerkort = null,
                innholdElementer = amoKategorisering.innholdElementer,
                norskprove = null,
            )

            is AmoKategorisering.Norskopplaering -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.NORSKOPPLAERING,
                bransje = null,
                sertifiseringer = null,
                forerkort = null,
                innholdElementer = amoKategorisering.innholdElementer,
                norskprove = amoKategorisering.norskprove,
            )

            AmoKategorisering.Studiespesialisering -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.STUDIESPESIALISERING,
                bransje = null,
                sertifiseringer = null,
                forerkort = null,
                innholdElementer = null,
                norskprove = null,
            )
        }
    }
}
