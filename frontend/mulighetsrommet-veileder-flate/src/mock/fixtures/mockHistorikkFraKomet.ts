import {
  AktivDeltakelse,
  Gruppetiltak,
  HistorikkForBrukerFraKomet,
} from "mulighetsrommet-api-client";

export const historikkFraKomet: HistorikkForBrukerFraKomet[] = [
  {
    periode: {
      startDato: "10.05.2023",
      sluttDato: "12.12.2023",
    },
    beskrivelse: null,
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Gruppetiltak.tiltakskode.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    historiskStatus: {
      historiskStatusType: HistorikkForBrukerFraKomet.historiskStatusType.HAR_SLUTTET,
    },
    innsoktDato: "03.02.2024",
  },
  {
    periode: {
      startDato: "01.01.2024",
      sluttDato: "01.02.2024",
    },
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    deltakerId: window.crypto.randomUUID(),
    historiskStatus: {
      historiskStatusType: HistorikkForBrukerFraKomet.historiskStatusType.FULLFORT,
    },

    beskrivelse: "Dårlig stemning mellom mentor og bruker",
    innsoktDato: "03.02.2024",
  },
  {
    periode: {
      startDato: "01.01.2024",
      sluttDato: "01.06.2025",
    },
    beskrivelse: null,
    tittel: "Jobbklubb - Oslo",
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Gruppetiltak.tiltakskode.JOBBK,
    },
    deltakerId: window.crypto.randomUUID(),
    historiskStatus: {
      historiskStatusType: HistorikkForBrukerFraKomet.historiskStatusType.IKKE_AKTUELL,
    },

    innsoktDato: "03.02.2024",
  },
];

export const utkastFraKomet: AktivDeltakelse[] = [
  {
    deltakerId: window.crypto.randomUUID(),
    beskrivelse: null,
    innsoktDato: "03.02.2024",
    sistEndretDato: "27.02.2024",
    aktivStatus: {
      navn: AktivDeltakelse.navn.KLADD,
    },
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    tittel: "Avklaring hos Muligheter AS",
  },
];
