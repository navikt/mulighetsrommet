import { AktivDeltakelse, Gruppetiltak, HistorikkForBrukerV2 } from "mulighetsrommet-api-client";

export const historikkFraKomet: HistorikkForBrukerV2[] = [
  {
    periode: {
      startdato: "10.05.2023",
      sluttdato: "12.12.2023",
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
    innsoktDato: "03.02.2024",
  },
  {
    periode: {
      startdato: "01.01.2024",
      sluttdato: "01.02.2024",
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
    innsoktDato: "03.02.2024",
  },
  {
    periode: {
      startdato: "01.01.2024",
      sluttdato: "01.06.2025",
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

    innsoktDato: "03.02.2024",
  },
];

export const utkastFraKomet: AktivDeltakelse[] = [
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "03.02.2024",
    sistEndretdato: "27.02.2024",
    aktivStatus: AktivDeltakelse.aktivStatus.KLADD,
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    tittel: "Avklaring hos Muligheter AS",
  },
];
