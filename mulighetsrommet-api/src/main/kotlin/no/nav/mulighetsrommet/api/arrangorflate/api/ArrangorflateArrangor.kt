package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class ArrangorflateArrangor(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
)
