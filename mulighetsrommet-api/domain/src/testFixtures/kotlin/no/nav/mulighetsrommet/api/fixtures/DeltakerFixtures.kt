package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.deltaker.Deltakelsesmengde
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.deltaker.NavVeileder
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

object DeltakerFixtures {
    fun createDeltaker(
        id: UUID = UUID.randomUUID(),
        gjennomforingId: UUID,
        status: DeltakerStatusType = DeltakerStatusType.DELTAR,
        startDato: LocalDate? = LocalDate.now(),
        sluttDato: LocalDate? = LocalDate.now().plusMonths(1),
        endretTidspunkt: Instant = Instant.now(),
        innhold: String? = null,
        veileder: NavVeileder? = null,
    ) = Deltaker.opprett(
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
        navVeileder = veileder,
    )

    fun createDeltakerMedDeltakelsesmengder(
        id: UUID = UUID.randomUUID(),
        gjennomforingId: UUID,
        startDato: LocalDate = LocalDate.now(),
        sluttDato: LocalDate? = LocalDate.now().plusMonths(1),
        status: DeltakerStatusType = DeltakerStatusType.DELTAR,
        endretTidspunkt: Instant = Instant.now(),
        deltakelsesprosent: Double = 100.0,
        deltakelsesmengder: List<Deltakelsesmengde> = listOf(
            Deltakelsesmengde(
                gyldigFra = startDato,
                deltakelsesprosent = deltakelsesprosent,
                opprettetTidspunkt = Instant.now(),
            ),
        ),
    ) = Deltaker.opprett(
        id = id,
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertTidspunkt = Instant.now(),
        endretTidspunkt = Instant.now(),
        deltakelsesmengder = deltakelsesmengder,
        status = DeltakerStatus(
            type = status,
            aarsak = null,
            opprettetTidspunkt = endretTidspunkt,
        ),
        innholdAnnet = null,
        navVeileder = null,
    )
}
