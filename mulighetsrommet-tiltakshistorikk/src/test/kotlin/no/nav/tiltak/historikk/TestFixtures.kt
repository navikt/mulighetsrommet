package no.nav.tiltak.historikk

import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.model.ArbeidsgiverAvtale
import no.nav.tiltak.historikk.model.Gjennomforing
import no.nav.tiltak.historikk.model.Gjennomforing.Type
import no.nav.tiltak.historikk.model.KometDeltaker
import no.nav.tiltak.historikk.model.Tiltakstype
import no.nav.tiltak.historikk.model.Virksomhet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TestFixtures {
    object Tiltakstype {
        val GruppeAmo = Tiltakstype(
            navn = "Arbeidsmarkedsopplæring (gruppe)",
            tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            arenaTiltakskode = "GRUPPEAMO",
            tiltakstypeId = UUID.fromString(
                "9b52265c-914c-413d-bca4-e9d7b3f1bd8d",
            ),
            opprettetTidspunkt = Instant.parse("2024-12-31T23:00:00Z"),
            oppdatertTidspunkt = Instant.parse("2025-01-01T23:00:00Z"),
        )

        val EnkelAmo = Tiltakstype(
            navn = "Arbeidsmarkedsopplæring (enkeltplass)",
            tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            arenaTiltakskode = "ENKELAMO",
            tiltakstypeId = UUID.fromString(
                "156f003f-e391-4b0d-9dba-ce398bf5fdde",
            ),
            opprettetTidspunkt = Instant.parse("2024-12-31T23:00:00Z"),
            oppdatertTidspunkt = Instant.parse("2025-01-01T23:00:00Z"),
        )

        val Mentor = Tiltakstype(
            navn = "Mentor",
            tiltakskode = Tiltakskode.MENTOR,
            arenaTiltakskode = "MENTOR",
            tiltakstypeId = UUID.fromString(
                "5d48aa11-b394-40ba-ae22-2b62f68f4191",
            ),
            opprettetTidspunkt = Instant.parse("2024-12-31T23:00:00Z"),
            oppdatertTidspunkt = Instant.parse("2025-01-01T23:00:00Z"),
        )

        val Arbeidstrening = Tiltakstype(
            navn = "Arbeidstrening",
            tiltakskode = Tiltakskode.ARBEIDSTRENING,
            arenaTiltakskode = "ARBTREN",
            tiltakstypeId = UUID.fromString(
                "c1cdf1ea-6d47-40f6-9787-d64670b5ae08",
            ),
            opprettetTidspunkt = Instant.parse("2024-12-31T23:00:00Z"),
            oppdatertTidspunkt = Instant.parse("2025-01-01T23:00:00Z"),
        )
    }

    object Virksomhet {
        val ArrangorHovedenhet = Virksomhet(
            organisasjonsnummer = Organisasjonsnummer("912345678"),
            overordnetEnhetOrganisasjonsnummer = null,
            navn = "Arrangør Foretak",
            organisasjonsform = "AS",
            slettetDato = null,
        )

        val Arrangor = Virksomhet(
            organisasjonsnummer = Organisasjonsnummer("987654321"),
            overordnetEnhetOrganisasjonsnummer = null,
            navn = "Arrangør",
            organisasjonsform = "BEDR",
            slettetDato = null,
        )

        val Arbeidsgiver = Virksomhet(
            organisasjonsnummer = Organisasjonsnummer("876543210"),
            overordnetEnhetOrganisasjonsnummer = null,
            navn = "Arbeidsgiver",
            organisasjonsform = "BEDR",
            slettetDato = null,
        )
    }

    object Gjennomforing {
        val GruppeAmo = Gjennomforing(
            id = UUID.fromString("86a263fa-1f30-4c0e-af64-2e7015e6daed"),
            type = Type.GRUPPE,
            tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            arrangorOrganisasjonsnummer = "987654321",
            navn = "Gruppe AMO",
            deltidsprosent = 80.0,
            opprettetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
            oppdatertTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
        )

        val EnkelAmo = Gjennomforing(
            id = UUID.fromString("bcd33931-0230-4b2b-a223-9dd4c140e949"),
            type = Type.ENKELTPLASS,
            tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            arrangorOrganisasjonsnummer = "987654321",
            navn = null,
            deltidsprosent = null,
            opprettetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
            oppdatertTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
        )
    }

    object Deltaker {
        val GruppeAmo = KometDeltaker(
            id = UUID.fromString("d702a2af-b091-41bb-b54b-c0e19312baee"),
            gjennomforingId = Gjennomforing.GruppeAmo.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            statusType = DeltakerStatusType.VENTER_PA_OPPSTART,
            statusAarsak = null,
            statusOpprettetTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
            opprettetTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
            oppdatertTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
            dagerPerUke = 2.5f,
            prosentStilling = 50f,
        )

        val EnkelAmo = KometDeltaker(
            id = UUID.fromString("9fe2abb2-569f-4bb5-b048-85b958f0470e"),
            gjennomforingId = Gjennomforing.EnkelAmo.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            statusType = DeltakerStatusType.VENTER_PA_OPPSTART,
            statusAarsak = null,
            statusOpprettetTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
            opprettetTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
            oppdatertTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
            dagerPerUke = 2.5f,
            prosentStilling = 50f,
        )
    }

    object ArenaGjennomforing {
        val Arbeidstrening = TiltakshistorikkArenaGjennomforing(
            id = UUID.randomUUID(),
            arenaRegDato = Instant.parse("2025-01-01T00:00:00Z"),
            arenaModDato = Instant.parse("2025-01-02T00:00:00Z"),
            arrangorOrganisasjonsnummer = Organisasjonsnummer("987654321"),
            navn = "Arbeidstrening hos Fretex",
            deltidsprosent = 80.0,
            tiltakstypeId = Tiltakstype.Arbeidstrening.tiltakstypeId,
        )

        val Mentor = TiltakshistorikkArenaGjennomforing(
            id = UUID.randomUUID(),
            arenaRegDato = Instant.parse("2025-01-01T00:00:00Z"),
            arenaModDato = Instant.parse("2025-01-02T00:00:00Z"),
            arrangorOrganisasjonsnummer = Organisasjonsnummer("987654321"),
            navn = "Mentortiltak hos Joblearn",
            deltidsprosent = 100.0,
            tiltakstypeId = Tiltakstype.Mentor.tiltakstypeId,
        )

        val EnkelAmo = TiltakshistorikkArenaGjennomforing(
            id = UUID.fromString("169dd79f-e65c-4454-bef4-fb9b352424c8"),
            arenaRegDato = Instant.parse("2024-01-01T00:00:00Z"),
            arenaModDato = Instant.parse("2024-01-01T00:00:00Z"),
            arrangorOrganisasjonsnummer = Organisasjonsnummer("987654321"),
            navn = "Enkelt-AMO hos Joblearn",
            deltidsprosent = 100.0,
            tiltakstypeId = Tiltakstype.EnkelAmo.tiltakstypeId,
        )
    }

    object ArenaDeltaker {
        val Arbeidstrening = TiltakshistorikkArenaDeltaker(
            id = UUID.fromString("970ef351-bb42-4560-b501-df1ed152fa2b"),
            arenaRegDato = Instant.parse("2024-01-01T12:00:00Z"),
            arenaModDato = Instant.parse("2024-02-01T12:00:00Z"),
            arenaGjennomforingId = ArenaGjennomforing.Arbeidstrening.id,
            arenaDeltakerId = 789,
            norskIdent = NorskIdent("10101010100"),
            status = ArenaDeltakerStatus.GJENNOMFORES,
            startDato = LocalDate.of(2024, 1, 1),
            sluttDato = LocalDate.of(2024, 12, 31),
            dagerPerUke = 5.0,
            deltidsprosent = 100.0,
        )

        val EnkelAmo = TiltakshistorikkArenaDeltaker(
            id = UUID.fromString("6a7c2961-df09-420b-ab05-65d34de7c2cf"),
            arenaRegDato = Instant.parse("2024-01-01T12:00:00Z"),
            arenaModDato = Instant.parse("2024-02-01T12:00:00Z"),
            arenaGjennomforingId = ArenaGjennomforing.EnkelAmo.id,
            arenaDeltakerId = 123,
            norskIdent = NorskIdent("10101010100"),
            status = ArenaDeltakerStatus.GJENNOMFORES,
            startDato = LocalDate.of(2024, 1, 1),
            sluttDato = LocalDate.of(2024, 12, 31),
            dagerPerUke = 5.0,
            deltidsprosent = 100.0,
        )
    }

    object Avtale {
        val Arbeidstrening = ArbeidsgiverAvtale(
            avtaleId = UUID.fromString("065e130b-5fc0-4df3-8483-165cd59c26d8"),
            norskIdent = NorskIdent("10101010100"),
            organisasjonsnummer = Virksomhet.Arbeidsgiver.organisasjonsnummer,
            tiltakstype = Tiltakskode.ARBEIDSTRENING,
            startDato = LocalDate.of(2024, 1, 1),
            sluttDato = LocalDate.of(2024, 12, 31),
            status = ArbeidsgiverAvtale.Status.GJENNOMFORES,
            stillingsprosent = 100f,
            dagerPerUke = 5f,
            opprettetTidspunkt = Instant.parse("2024-01-01T12:00:00Z"),
            oppdatertTidspunkt = Instant.parse("2024-02-01T12:00:00Z"),
        )
    }
}
