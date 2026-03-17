package no.nav.mulighetsrommet.api.vedtak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class Opplaeringtilskudd(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val kode: Kode,
) {
    enum class Kode {
        SKOLEPENGER,
        STUDIEREISE,
        EKSAMENSAVGIFT,
        SEMESTERAVGIFT,
        INTEGRERT_BOTILBUD,
    }
}
