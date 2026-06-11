package no.nav.mulighetsrommet.api.tiltakstype.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class RedaksjoneltInnholdLenke(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val url: String,
    val navn: String?,
    val beskrivelse: String? = null,
)
