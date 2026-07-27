package no.nav.mulighetsrommet.api.deltaker.kafka

import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object AmtDeltakerEksternV1DtoFixtures {
    fun createAmtDeltakerDto(
        id: UUID = UUID.randomUUID(),
        gjennomforingId: UUID,
        status: DeltakerStatusType,
        personIdent: String,
        endretTidspunkt: LocalDateTime = LocalDateTime.now(),
        startDato: LocalDate? = null,
        sluttDato: LocalDate? = null,
    ) = AmtDeltakerEksternV1Dto(
        id = id,
        gjennomforingId = gjennomforingId,
        personIdent = personIdent,
        startDato = startDato,
        sluttDato = sluttDato,
        status = createAmtDeltakerStatusDto(status, endretTidspunkt),
        registrertTidspunkt = endretTidspunkt,
        endretTidspunkt = endretTidspunkt,
        deltakelsesmengder = listOf(),
        kilde = AmtDeltakerEksternV1Dto.Kilde.KOMET,
        innhold = AmtDeltakerEksternV1Dto.DeltakelsesinnholdDto(
            ledetekst = null,
            valgtInnhold = listOf(
                AmtDeltakerEksternV1Dto.InnholdDto(
                    innholdskode = "annet",
                    tekst = "Prisinformasjon",
                ),
            ),
        ),
        navVeileder = null,
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
