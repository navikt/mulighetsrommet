package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class ArrangorflateGjennomforingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val lopenummer: Tiltaksnummer,
)
