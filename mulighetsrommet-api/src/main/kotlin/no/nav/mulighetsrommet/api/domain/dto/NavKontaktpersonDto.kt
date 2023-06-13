package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class NavKontaktpersonDto(
    @Serializable(with = UUIDSerializer::class)
    val azureId: UUID,
    val navident: String,
    val fornavn: String,
    val etternavn: String,
    val hovedenhetKode: String,
    val mobilnr: String? = null,
    val epost: String,
)
