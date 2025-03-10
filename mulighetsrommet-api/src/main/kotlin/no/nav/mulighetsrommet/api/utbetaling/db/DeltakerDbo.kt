package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.model.DeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class DeltakerDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val registrertTidspunkt: LocalDateTime,
    val endretTidspunkt: LocalDateTime,
    val deltakelsesprosent: Double?,
    val status: DeltakerStatus,
    val deltakelsesmengder: List<Deltakelsesmengde>,
) {
    data class Deltakelsesmengde(
        val gyldigFra: LocalDate,
        val deltakelsesprosent: Double,
        val opprettetTidspunkt: LocalDateTime,
    )
}
