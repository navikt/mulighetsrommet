package no.nav.mulighetsrommet.api.domain.opplaring

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class ForerkortKlasse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val kode: Kode,
    val navn: String,
) {
    enum class Kode {
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
