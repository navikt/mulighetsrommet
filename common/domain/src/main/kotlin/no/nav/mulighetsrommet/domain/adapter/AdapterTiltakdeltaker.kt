package no.nav.mulighetsrommet.domain.adapter

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.DateSerializer
import no.nav.mulighetsrommet.domain.models.Deltakerstatus
import java.time.LocalDateTime

@Serializable
data class AdapterTiltakdeltaker (
    val id: Int,
    val tiltaksgjennomforingId: Int,
    val personId: Int,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus
)
