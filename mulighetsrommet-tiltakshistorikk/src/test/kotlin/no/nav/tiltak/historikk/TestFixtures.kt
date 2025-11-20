package no.nav.tiltak.historikk

import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TestFixtures {
    val virksomhet = VirksomhetDbo(
        organisasjonsnummer = Organisasjonsnummer("987654321"),
        overordnetEnhetOrganisasjonsnummer = null,
        navn = "Arrangør",
        organisasjonsform = "BEDR",
        slettetDato = null,
    )

    val gjennomforingGruppe: TiltaksgjennomforingV2Dto.Gruppe = TiltaksgjennomforingV2Dto.Gruppe(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(
            organisasjonsnummer = Organisasjonsnummer("987654321"),
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
            organisasjonsnummer = Organisasjonsnummer("987654321"),
        ),
        opprettetTidspunkt = Instant.now(),
        oppdatertTidspunkt = Instant.now(),
    )

    val amtDeltaker = AmtDeltakerV1Dto(
        id = UUID.randomUUID(),
        gjennomforingId = gjennomforingGruppe.id,
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
        prosentStilling = 50f,
        deltakelsesmengder = listOf(),
    )
}
