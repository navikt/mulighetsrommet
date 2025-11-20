package no.nav.tiltak.historikk

import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.historikk.db.queries.TiltakstypeDbo
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object TestFixtures {
    object Tiltakstype {
        val gruppeAmo = TiltakstypeDbo(
            navn = "Arbeidsmarkedsopplæring (gruppe)",
            tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name,
            arenaTiltakskode = "GRUPPEAMO",
        )

        val enkelAmo = TiltakstypeDbo(
            navn = "Arbeidsmarkedsopplæring (enkeltplass)",
            tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING.name,
            arenaTiltakskode = "ENKELAMO",
        )
    }

    object Virksomhet {
        val arrangor = VirksomhetDbo(
            organisasjonsnummer = Organisasjonsnummer("987654321"),
            overordnetEnhetOrganisasjonsnummer = null,
            navn = "Arrangør",
            organisasjonsform = "BEDR",
            slettetDato = null,
        )

        val arbeidsgiver = VirksomhetDbo(
            organisasjonsnummer = Organisasjonsnummer("876543210"),
            overordnetEnhetOrganisasjonsnummer = null,
            navn = "Arbeidsgiver",
            organisasjonsform = "BEDR",
            slettetDato = null,
        )
    }

    object Gjennomforing {
        val gruppeAmo: TiltaksgjennomforingV2Dto.Gruppe = TiltaksgjennomforingV2Dto.Gruppe(
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

        val enkelAmo: TiltaksgjennomforingV2Dto.Enkeltplass = TiltaksgjennomforingV2Dto.Enkeltplass(
            id = UUID.randomUUID(),
            tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            arrangor = TiltaksgjennomforingV2Dto.Arrangor(
                organisasjonsnummer = Organisasjonsnummer("987654321"),
            ),
            opprettetTidspunkt = Instant.now(),
            oppdatertTidspunkt = Instant.now(),
        )

        val arenaArbeidstrening = TiltakshistorikkArenaGjennomforing(
            id = UUID.randomUUID(),
            arenaTiltakskode = "ARBTREN",
            arenaRegDato = LocalDate.of(2025, 1, 1).atStartOfDay(),
            arenaModDato = LocalDate.of(2025, 1, 2).atStartOfDay(),
            arrangorOrganisasjonsnummer = Organisasjonsnummer("987654321"),
            navn = "Arbeidstrening hos Fretex",
            deltidsprosent = 80.0,
        )

        val arenaMentor = TiltakshistorikkArenaGjennomforing(
            id = UUID.randomUUID(),
            arenaTiltakskode = "MENTOR",
            arenaRegDato = LocalDate.of(2025, 1, 1).atStartOfDay(),
            arenaModDato = LocalDate.of(2025, 1, 2).atStartOfDay(),
            arrangorOrganisasjonsnummer = Organisasjonsnummer("987654321"),
            navn = "Mentortiltak hos Joblearn",
            deltidsprosent = 100.0,
        )

        val arenaAmo = TiltakshistorikkArenaGjennomforing(
            id = UUID.randomUUID(),
            arenaTiltakskode = "AMO",
            arenaRegDato = LocalDate.of(2024, 1, 1).atStartOfDay(),
            arenaModDato = LocalDate.of(2024, 1, 1).atStartOfDay(),
            arrangorOrganisasjonsnummer = Organisasjonsnummer("987654321"),
            navn = "Enkelt-AMO hos Joblearn",
            deltidsprosent = 100.0,
        )
    }

    object Deltaker {
        val gruppeAmo = AmtDeltakerV1Dto(
            id = UUID.randomUUID(),
            gjennomforingId = Gjennomforing.gruppeAmo.id,
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
}
