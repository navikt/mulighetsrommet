package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class AzureAdNavAnsatt(
    @Serializable(with = UUIDSerializer::class)
    val azureId: UUID,
    val navIdent: String,
    val fornavn: String,
    val etternavn: String,
    val hovedenhetKode: String,
    val hovedenhetNavn: String,
    val mobilnummer: String?,
    val epost: String,
)
