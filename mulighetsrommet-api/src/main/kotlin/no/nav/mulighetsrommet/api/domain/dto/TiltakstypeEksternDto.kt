package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class TiltakstypeEksternDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val arenaKode: String?,
    val deltakerRegistreringInnhold: DeltakerRegistreringInnholdDto?,
)
