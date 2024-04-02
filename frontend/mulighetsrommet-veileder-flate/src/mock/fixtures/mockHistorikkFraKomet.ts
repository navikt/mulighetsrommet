import { AktivDeltakelse, Gruppetiltak, HistorikkForBrukerV2 } from "mulighetsrommet-api-client";

export const utkastFraKomet: AktivDeltakelse[] = [
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    sistEndretdato: "2024-03-27",
    aktivStatus: AktivDeltakelse.aktivStatus.DELTAR,
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Gruppetiltak.tiltakskode.JOBBK,
    },
    tittel: "Jobbklubb hos Fretex",
  },
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
];

export const historikkFraKomet: HistorikkForBrukerV2[] = [
  {
    periode: {
      startdato: "2023-05-10",
      sluttdato: "2023-12-12",
    },
    beskrivelse: null,
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

    beskrivelse: "Dårlig stemning mellom mentor og bruker",
    innsoktDato: "2024-02-03",
  },
  {
    periode: {
      startdato: "2024-01-01",
      sluttdato: "2025-06-01",
    },
    beskrivelse: null,
    tittel: "Jobbklubb - Oslo",
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Gruppetiltak.tiltakskode.JOBBK,
    },
    deltakerId: window.crypto.randomUUID(),
    historiskStatus: {
      historiskStatusType: HistorikkForBrukerV2.historiskStatusType.IKKE_AKTUELL,
    },

    innsoktDato: "2024-02-03",
  },
];
