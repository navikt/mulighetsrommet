package no.nav.mulighetsrommet.api.fixtures

import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object DeltakerFixtures {
    fun createDeltakerMedDeltakelsesmengderDbo(
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
        registrertTidspunkt = LocalDateTime.now(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesmengder = deltakelsesmengder,
        status = DeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetTidspunkt = statusOpprettet,
        ),
    )

    fun createDeltakerDbo(
        gjennomforingId: UUID,
        startDato: LocalDate = LocalDate.now(),
        sluttDato: LocalDate? = LocalDate.now().plusMonths(1),
        statusType: DeltakerStatusType = DeltakerStatusType.DELTAR,
        statusOpprettet: LocalDateTime = LocalDateTime.now(),
    ) = DeltakerDbo(
        id = UUID.randomUUID(),
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertTidspunkt = LocalDateTime.now(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesmengder = emptyList(),
        status = DeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetTidspunkt = statusOpprettet,
        ),
    )

    fun createDeltaker(
        gjennomforingId: UUID = UUID.randomUUID(),
        startDato: LocalDate?,
        sluttDato: LocalDate?,
        statusType: DeltakerStatusType,
    ) = Deltaker(
        id = UUID.randomUUID(),
        startDato = startDato,
        sluttDato = sluttDato,
        gjennomforingId = gjennomforingId,
        registrertTidspunkt = LocalDateTime.now(),
        endretTidspunkt = LocalDateTime.now(),
        status = DeltakerStatus(
            type = statusType,
            aarsak = null,
            opprettetTidspunkt = LocalDateTime.now(),
        ),
        deltakelsesmengder = listOf(),
    )

    fun createAmtDeltakerDto(
        id: UUID = UUID.randomUUID(),
        gjennomforingId: UUID,
        status: DeltakerStatusType,
        personIdent: String,
        opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    ) = AmtDeltakerEksternV1Dto(
        id = id,
        gjennomforingId = gjennomforingId,
        personIdent = personIdent,
        startDato = null,
        sluttDato = null,
        status = createAmtDeltakerStatusDto(status, opprettetTidspunkt),
        registrertTidspunkt = opprettetTidspunkt,
        endretTidspunkt = opprettetTidspunkt,
        deltakelsesmengder = listOf(),
        kilde = AmtDeltakerEksternV1Dto.Kilde.KOMET,
        innhold = AmtDeltakerEksternV1Dto.DeltakelsesinnholdDto(
            ledetekst = null,
            valgtInnhold = listOf(),
        ),
    )

    fun createAmtDeltakerStatusDto(
        type: DeltakerStatusType,
        opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    ): AmtDeltakerEksternV1Dto.StatusDto = AmtDeltakerEksternV1Dto.StatusDto(
        type = type,
        tekst = type.description,
        aarsak = AmtDeltakerEksternV1Dto.AarsakDto(null, null),
        opprettetTidspunkt = opprettetTidspunkt,
    )
}
