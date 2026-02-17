package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Deltaker(
    val id: UUID,
    val gjennomforingId: UUID,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val registrertTidspunkt: LocalDateTime,
    val endretTidspunkt: LocalDateTime,
    val status: DeltakerStatus,
    val deltakelsesmengder: List<Deltakelsesmengde>,
)

@Serializable
data class Deltakelsesmengde(
    @Serializable(with = LocalDateSerializer::class)
    val gyldigFra: LocalDate,
    val deltakelsesprosent: Double,
)
