package no.nav.mulighetsrommet.api.lagretfilter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

enum class FilterDokumentType {
    AVTALE,
    GJENNOMFORING,
    GJENNOMFORING_MODIA,
}

@Serializable
data class LagretFilterDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val brukerId: String,
    val navn: String,
    val type: FilterDokumentType,
    val filter: JsonElement,
    val sortOrder: Int,
)
