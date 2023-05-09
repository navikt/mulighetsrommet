package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class AdGruppe(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
)
