package no.nav.mulighetsrommet.api.persistence.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
internal data class TiltakstypeDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val arenaKode: String?,
)
