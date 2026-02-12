package no.nav.tiltak.historikk

import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.db.queries.TiltakstypeDbo
import no.nav.tiltak.historikk.db.queries.VirksomhetDbo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TestFixtures {
    object Tiltakstype {
        val gruppeAmo = TiltakstypeDbo(
            navn = "Arbeidsmarkedsopplæring (gruppe)",
            tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name,
            arenaTiltakskode = "GRUPPEAMO",
            tiltakstypeId = UUID.fromString(
                "9b52265c-914c-413d-bca4-e9d7b3f1bd8d",
            ),
        )

        val enkelAmo = TiltakstypeDbo(
            navn = "Arbeidsmarkedsopplæring (enkeltplass)",
            tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING.name,
            arenaTiltakskode = "ENKELAMO",
            tiltakstypeId = UUID.fromString(
                "156f003f-e391-4b0d-9dba-ce398bf5fdde",
            ),
        )

        val amo = TiltakstypeDbo(
            navn = "Arbeidsmarkedsopplæring (AMO)",
            tiltakskode = null,
            arenaTiltakskode = "AMO",
            tiltakstypeId = UUID.fromString(
                "d5f724d6-4779-41f8-b9e6-e54cb6486b93",
            ),
        )

        val mentor = TiltakstypeDbo(
            navn = "Mentor",
            tiltakskode = "MENTOR",
            arenaTiltakskode = "MENTOR",
            tiltakstypeId = UUID.fromString(
                "5d48aa11-b394-40ba-ae22-2b62f68f4191",
            ),
        )

        val arbeidstrening = TiltakstypeDbo(
            navn = "Arbeidstrening",
            tiltakskode = "ARBEIDSTRENING",
            arenaTiltakskode = "ARBTREN",
            tiltakstypeId = UUID.fromString(
                "c1cdf1ea-6d47-40f6-9787-d64670b5ae08",
            ),
        )
    }

    object Virksomhet {
        val arrangorHovedenhet = VirksomhetDbo(
            organisasjonsnummer = Organisasjonsnummer("912345678"),
            overordnetEnhetOrganisasjonsnummer = null,
            navn = "Arrangør Foretak",
            organisasjonsform = "AS",
            slettetDato = null,
        )

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
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
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
            tiltakstypeId = Tiltakstype.arbeidstrening.tiltakstypeId,
        )

        val arenaMentor = TiltakshistorikkArenaGjennomforing(
            id = UUID.randomUUID(),
            arenaTiltakskode = "MENTOR",
            arenaRegDato = LocalDate.of(2025, 1, 1).atStartOfDay(),
            arenaModDato = LocalDate.of(2025, 1, 2).atStartOfDay(),
            arrangorOrganisasjonsnummer = Organisasjonsnummer("987654321"),
            navn = "Mentortiltak hos Joblearn",
            deltidsprosent = 100.0,
            tiltakstypeId = Tiltakstype.mentor.tiltakstypeId,
        )

        val arenaAmo = TiltakshistorikkArenaGjennomforing(
            id = UUID.randomUUID(),
            arenaTiltakskode = "AMO",
            arenaRegDato = LocalDate.of(2024, 1, 1).atStartOfDay(),
            arenaModDato = LocalDate.of(2024, 1, 1).atStartOfDay(),
            arrangorOrganisasjonsnummer = Organisasjonsnummer("987654321"),
            navn = "Enkelt-AMO hos Joblearn",
            deltidsprosent = 100.0,
            tiltakstypeId = Tiltakstype.amo.tiltakstypeId,
        )
    }

    object Deltaker {
        val gruppeAmo = AmtDeltakerV1Dto(
            id = UUID.randomUUID(),
            gjennomforingId = Gjennomforing.gruppeAmo.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            status = AmtDeltakerV1Dto.DeltakerStatusDto(
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
