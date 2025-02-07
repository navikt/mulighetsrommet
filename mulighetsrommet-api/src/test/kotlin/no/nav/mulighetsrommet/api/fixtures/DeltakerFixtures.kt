package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.model.DeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object DeltakerFixtures {
    fun createDeltaker(
        gjennomforingId: UUID,
        startDato: LocalDate = LocalDate.now(),
        sluttDato: LocalDate = LocalDate.now().plusMonths(1),
        statusType: DeltakerStatus.Type = DeltakerStatus.Type.DELTAR,
        deltakelsesprosent: Double = 100.0,
    ) = DeltakerDbo(
        id = UUID.randomUUID(),
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertTidspunkt = LocalDateTime.now(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesprosent = deltakelsesprosent,
        status = DeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetDato = LocalDateTime.now(),
        ),
    )
}
