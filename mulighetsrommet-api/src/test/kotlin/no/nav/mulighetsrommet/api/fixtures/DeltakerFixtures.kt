package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object DeltakerFixtures {
    fun createDeltakerDbo(
        gjennomforingId: UUID,
        startDato: LocalDate = LocalDate.now(),
        sluttDato: LocalDate? = LocalDate.now().plusMonths(1),
        statusType: DeltakerStatusType = DeltakerStatusType.DELTAR,
        statusOpprettet: LocalDateTime = LocalDateTime.now(),
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
            opprettetDato = statusOpprettet,
        ),
    )

    fun createDeltakerDbo(
        gjennomforingId: UUID,
        startDato: LocalDate,
        sluttDato: LocalDate?,
        statusType: DeltakerStatusType,
        statusOpprettet: LocalDateTime = LocalDateTime.now(),
    ) = DeltakerDbo(
        id = UUID.randomUUID(),
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertDato = LocalDate.now(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesprosent = null,
        deltakelsesmengder = emptyList(),
        status = DeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetDato = statusOpprettet,
        ),
    )

    fun createDeltaker(
        gjennomforingId: UUID = UUID.randomUUID(),
        startDato: LocalDate?,
        sluttDato: LocalDate?,
        statusType: DeltakerStatusType,
        norskIdent: NorskIdent = NorskIdent("01010199999"),
    ) = Deltaker(
        id = UUID.randomUUID(),
        norskIdent = norskIdent,
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertDato = LocalDate.now(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesprosent = null,
        status = DeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetDato = LocalDateTime.now(),
        ),
    )
}
