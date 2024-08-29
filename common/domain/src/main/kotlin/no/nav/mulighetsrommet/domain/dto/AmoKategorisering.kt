package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

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
}
