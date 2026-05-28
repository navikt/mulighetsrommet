package no.nav.mulighetsrommet.api.amo.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class Kurstype(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val kode: Kode,
    val navn: String,
    val aktiv: Boolean,
) {
    enum class Kode {
        NORSKOPPLAERING,
        GRUNNLEGGENDE_FERDIGHETER,
        FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
        BRANSJE_OG_YRKESRETTET,
        STUDIESPESIALISERING,
    }
}
