package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object DeltakerFixtures {
    fun createDeltaker(
        gjennomforingId: UUID,
        startDato: LocalDate = LocalDate.now(),
        sluttDato: LocalDate = LocalDate.now().plusMonths(1),
        statusType: AmtDeltakerStatus.Type = AmtDeltakerStatus.Type.DELTAR,
        stillingsprosent: Double = 100.0,
    ) = DeltakerDbo(
        id = UUID.randomUUID(),
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertTidspunkt = LocalDateTime.now(),
        endretTidspunkt = LocalDateTime.now(),
        stillingsprosent = stillingsprosent,
        status = AmtDeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetDato = LocalDateTime.now(),
        ),
    )
}
