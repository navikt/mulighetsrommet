package no.nav.mulighetsrommet.api.lagretfilter

import java.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.serializers.UUIDSerializer

enum class LagretFilterType {
    AVTALE,
    GJENNOMFORING,
    GJENNOMFORING_MODIA,
    OPPGAVE,
    INNSENDING,
}

@Serializable
data class LagretFilter(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val type: LagretFilterType,
    val filter: JsonElement,
    val isDefault: Boolean = false,
    val sortOrder: Int,
)
