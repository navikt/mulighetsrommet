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
        statusType: DeltakerStatus.DeltakerStatusType = DeltakerStatus.DeltakerStatusType.DELTAR,
        deltakelsesprosent: Double = 100.0,
        deltakelsesmengder: List<DeltakerDbo.Deltakelsesmengde> = listOf(
            DeltakerDbo.Deltakelsesmengde(
                gyldigFra = startDato,
                deltakelsesprosent = deltakelsesprosent,
                opprettetTidspunkt = LocalDateTime.now(),
            ),
        ),
    ) = DeltakerDbo(
        id = UUID.randomUUID(),
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertDato = LocalDate.now(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesprosent = deltakelsesprosent,
        deltakelsesmengder = deltakelsesmengder,
        status = DeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetDato = LocalDateTime.now(),
        ),
    )

    fun createDeltaker(
        gjennomforingId: UUID,
        startDato: LocalDate,
        sluttDato: LocalDate,
        statusType: DeltakerStatus.DeltakerStatusType,
    ) = DeltakerDbo(
        id = UUID.randomUUID(),
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertDato = LocalDate.now(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesprosent = null,
        deltakelsesmengder = listOf(),
        status = DeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetDato = LocalDateTime.now(),
        ),
    )
}
