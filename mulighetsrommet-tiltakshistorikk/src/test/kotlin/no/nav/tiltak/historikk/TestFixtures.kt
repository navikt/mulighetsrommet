package no.nav.tiltak.historikk

import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TestFixtures {
    val gjennomforingGruppe: TiltaksgjennomforingV2Dto.Gruppe = TiltaksgjennomforingV2Dto.Gruppe(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(
            organisasjonsnummer = Organisasjonsnummer("123123123"),
        ),
        navn = "Gruppe AMO",
        startDato = LocalDate.now(),
        sluttDato = null,
        status = GjennomforingStatusType.GJENNOMFORES,
        oppstart = GjennomforingOppstartstype.FELLES,
        tilgjengeligForArrangorFraOgMedDato = null,
        apentForPamelding = true,
        antallPlasser = 10,
        deltidsprosent = 80.0,
        opprettetTidspunkt = Instant.now(),
        oppdatertTidspunkt = Instant.now(),
        oppmoteSted = null,
    )

    val gjennomforingEnkeltplass: TiltaksgjennomforingV2Dto.Enkeltplass = TiltaksgjennomforingV2Dto.Enkeltplass(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(
            organisasjonsnummer = Organisasjonsnummer("123123123"),
        ),
        opprettetTidspunkt = Instant.now(),
        oppdatertTidspunkt = Instant.now(),
    )

    val tiltak = TiltaksgjennomforingV1Dto(
        id = UUID.randomUUID(),
        tiltakstype = TiltaksgjennomforingV1Dto.Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Gruppe AMO",
            arenaKode = "GRUPPEAMO",
            tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        ),
        navn = "Gruppe AMO",
        virksomhetsnummer = "123123123",
        startDato = LocalDate.now(),
        sluttDato = null,
        status = GjennomforingStatusType.GJENNOMFORES,
        oppstart = GjennomforingOppstartstype.FELLES,
        tilgjengeligForArrangorFraOgMedDato = null,
        apentForPamelding = true,
        antallPlasser = 10,
        deltidsprosent = 100.0,
        opprettetTidspunkt = LocalDateTime.now(),
        oppdatertTidspunkt = LocalDateTime.now(),
        oppmoteSted = null,
    )

    val amtDeltaker = AmtDeltakerV1Dto(
        id = UUID.randomUUID(),
        gjennomforingId = tiltak.id,
        personIdent = "10101010100",
        startDato = null,
        sluttDato = null,
        status = DeltakerStatus(
            type = DeltakerStatusType.VENTER_PA_OPPSTART,
            aarsak = null,
            opprettetDato = LocalDateTime.of(2022, 1, 1, 0, 0),
        ),
        registrertDato = LocalDateTime.of(2022, 1, 1, 0, 0),
        endretDato = LocalDateTime.of(2022, 1, 1, 0, 0),
        dagerPerUke = 2.5f,
        prosentStilling = null,
        deltakelsesmengder = listOf(),
    )
}
