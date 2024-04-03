import { AktivDeltakelse, Gruppetiltak, HistorikkForBrukerV2 } from "mulighetsrommet-api-client";

export const utkastFraKomet: AktivDeltakelse[] = [
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-03-02",
    sistEndretdato: "2024-03-27",
    aktivStatus: AktivDeltakelse.aktivStatus.KLADD,
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    tittel: "Avklaring hos Muligheter AS",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    sistEndretdato: "2024-03-27",
    aktivStatus: AktivDeltakelse.aktivStatus.UTKAST_TIL_PAMELDING,
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    tittel: "Avklaring hos Fretex AS",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    sistEndretdato: "2024-03-27",
    aktivStatus: AktivDeltakelse.aktivStatus.VENTER_PA_OPPSTART,
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Gruppetiltak.tiltakskode.JOBBK,
    },
    tittel: "Jobbklubb hos Fretex",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    sistEndretdato: "2024-03-27",
    aktivStatus: AktivDeltakelse.aktivStatus.VENTER_PA_OPPSTART,
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Gruppetiltak.tiltakskode.JOBBK,
    },
    periode: {
      startdato: "2023-08-10",
      sluttdato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    sistEndretdato: "2024-03-27",
    aktivStatus: AktivDeltakelse.aktivStatus.DELTAR,
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Gruppetiltak.tiltakskode.JOBBK,
    },
    periode: {
      startdato: "2023-08-10",
      sluttdato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
  },
];

export const historikkFraKomet: HistorikkForBrukerV2[] = [
  {
    beskrivelse: null,
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Gruppetiltak.tiltakskode.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    historiskStatus: {
      historiskStatusType: HistorikkForBrukerV2.historiskStatusType.AVBRUTT_UTKAST,
    },
    sistEndretdato: "2024-03-07",
    innsoktDato: "2024-03-02",
  },
  {
    beskrivelse: "utdanning",
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Gruppetiltak.tiltakskode.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    historiskStatus: {
      historiskStatusType: HistorikkForBrukerV2.historiskStatusType.IKKE_AKTUELL,
      aarsak: "utdanning",
    },
    sistEndretdato: "2024-03-07",
    innsoktDato: "2024-03-02",
  },
  {
    periode: {
      startdato: "2023-05-10",
      sluttdato: "2023-12-12",
    },
    beskrivelse: "fått jobb",
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Gruppetiltak.tiltakskode.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    historiskStatus: {
      historiskStatusType: HistorikkForBrukerV2.historiskStatusType.HAR_SLUTTET,
    },
    innsoktDato: "2024-03-02",
  },
  {
    periode: {
      startdato: "2023-01.01",
      sluttdato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    deltakerId: window.crypto.randomUUID(),
    historiskStatus: {
      historiskStatusType: HistorikkForBrukerV2.historiskStatusType.FULLFORT,
    },
    beskrivelse: null,
    innsoktDato: "2024-02-03",
  },
];
