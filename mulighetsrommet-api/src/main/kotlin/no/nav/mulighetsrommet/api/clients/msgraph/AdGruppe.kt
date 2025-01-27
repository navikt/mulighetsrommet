package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class AdGruppe(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
)
