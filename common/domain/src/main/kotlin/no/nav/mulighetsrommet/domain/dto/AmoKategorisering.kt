package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class AmoKategorisering(
    val kurstype: Kurstype,
    val spesifisering: Spesifisering? = null,
    val norskprove: Boolean? = null,
    val forerkort: List<ForerkortKlasse>? = emptyList(),
    val sertifiseringer: List<Sertifisering>? = emptyList(),
    val innholdElementer: List<InnholdElement>? = emptyList(),
) {
    enum class Kurstype {
        BRANSJE,
        NORSKOPPLAERING,
        STUDIESPESIALISERING,
    }

    enum class Spesifisering {
        SERVERING_OVERNATTING,
        TRANSPORT,
        INDUSTRI,
        ANDRE_BRANSJER,
        NORSKOPPLAERING,
        GRUNNLEGGENDE_FERDIGHETER,
        FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
    }

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

    enum class InnholdElement {
        GRUNNLEGGENDE_FERDIGHETER,
        JOBBSOKER_KOMPETANSE,
        TEORETISK_OPPLAERING,
        PRAKTISK_OPPLAERING,
        ARBEIDSLIVSKUNNSKAP,
    }

    @Serializable
    data class Sertifisering(val konseptId: Long, val label: String)
}