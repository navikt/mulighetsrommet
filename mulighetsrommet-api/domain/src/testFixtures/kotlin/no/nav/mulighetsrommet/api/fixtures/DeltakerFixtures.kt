package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.deltaker.Deltakelsesmengde
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object DeltakerFixtures {
    fun createDeltaker(
        id: UUID = UUID.randomUUID(),
        gjennomforingId: UUID,
        status: DeltakerStatusType = DeltakerStatusType.DELTAR,
        startDato: LocalDate? = LocalDate.now(),
        sluttDato: LocalDate? = LocalDate.now().plusMonths(1),
        endretTidspunkt: LocalDateTime = LocalDateTime.now(),
        innhold: String? = null,
    ) = Deltaker(
        id = id,
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertTidspunkt = endretTidspunkt,
        endretTidspunkt = endretTidspunkt,
        status = DeltakerStatus(
            type = status,
            aarsak = null,
            opprettetTidspunkt = endretTidspunkt,
        ),
        deltakelsesmengder = listOf(),
        innholdAnnet = innhold,
        navVeileder = null,
    )

    fun createDeltakerMedDeltakelsesmengder(
        gjennomforingId: UUID,
        startDato: LocalDate = LocalDate.now(),
        sluttDato: LocalDate? = LocalDate.now().plusMonths(1),
        statusType: DeltakerStatusType = DeltakerStatusType.DELTAR,
        statusOpprettet: LocalDateTime = LocalDateTime.now(),
        deltakelsesprosent: Double = 100.0,
        deltakelsesmengder: List<Deltakelsesmengde> = listOf(
            Deltakelsesmengde(
                gyldigFra = startDato,
                deltakelsesprosent = deltakelsesprosent,
                opprettetTidspunkt = LocalDateTime.now(),
            ),
        ),
    ) = Deltaker(
        id = UUID.randomUUID(),
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertTidspunkt = LocalDateTime.now(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesmengder = deltakelsesmengder,
        status = DeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetTidspunkt = statusOpprettet,
        ),
        innholdAnnet = null,
        navVeileder = null,
    )
}
