package no.nav.mulighetsrommet.admin.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TiltakstypeKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val gruppe: String?,
    val features: Set<TiltakstypeFeature>,
    val egenskaper: Set<TiltakstypeEgenskap>,
)
