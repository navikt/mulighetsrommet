package no.nav.mulighetsrommet.api.domain.opplaring

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class InnholdElement(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val kode: Kode,
    val navn: String,
) {
    enum class Kode {
        GRUNNLEGGENDE_FERDIGHETER,
        BRANSJERETTET_OPPLARING,
        TEORETISK_OPPLAERING,
        JOBBSOKER_KOMPETANSE,
        PRAKSIS,
        ARBEIDSMARKEDSKUNNSKAP,
        NORSKOPPLAERING,
    }
}
