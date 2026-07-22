package no.nav.mulighetsrommet.api.domain.deltaker

import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
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
    val innholdAnnet: String?,
    val navVeileder: NavVeileder?,
) {
    fun erFeilregistrert(): Boolean = status.type == DeltakerStatusType.FEILREGISTRERT
}
