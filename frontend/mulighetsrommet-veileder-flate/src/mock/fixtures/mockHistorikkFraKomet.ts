import { HistorikkForBrukerFraKomet, UtkastForBrukerFraKomet } from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "./mockTiltaksgjennomforinger";

export const historikkFraKomet: HistorikkForBrukerFraKomet[] = [
  {
    fraDato: "10.05.2023",
    tilDato: "12.12.2023",
    beskrivelse: null,
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: "Oppfølging",
    id: window.crypto.randomUUID(),
    status: {
      navn: HistorikkForBrukerFraKomet.navn.AVSLUTTET,
    },
    tiltaksgjennomforingId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    innsoktDato: "03.02.2024",
  },
  {
    fraDato: "01.01.2024",
    tilDato: "01.02.2024",
    tittel: "Mentor med Manfred",
    tiltakstype: "Mentor",
    id: window.crypto.randomUUID(),
    status: {
      navn: HistorikkForBrukerFraKomet.navn.AVSLUTTET,
    },
    tiltaksgjennomforingId:
      mockTiltaksgjennomforinger[2].id ||
      mockTiltaksgjennomforinger[2].sanityId ||
      window.crypto.randomUUID(),
    beskrivelse: "Dårlig stemning mellom mentor og bruker",
    innsoktDato: "03.02.2024",
  },
  {
    fraDato: "01.01.2024",
    tilDato: "01.06.2025",
    beskrivelse: null,
    tittel: "Arbeidstrening hos Jobbfrukt ASA",
    tiltakstype: "Arbeidstrening",
    id: window.crypto.randomUUID(),
    status: {
      navn: HistorikkForBrukerFraKomet.navn.IKKE_AKTUELL,
    },
    tiltaksgjennomforingId:
      mockTiltaksgjennomforinger[1].id ||
      mockTiltaksgjennomforinger[1].sanityId ||
      window.crypto.randomUUID(),
    innsoktDato: "03.02.2024",
  },
];

export const utkastFraKomet: UtkastForBrukerFraKomet[] = [
  {
    id: window.crypto.randomUUID(),
    beskrivelse: null,
    innsoktDato: "03.02.2024",
    sistEndretDato: "27.02.2024",
    status: {
      navn: UtkastForBrukerFraKomet.navn.KLADD,
    },
    tiltaksgjennomforingId:
      mockTiltaksgjennomforinger[2].id ||
      mockTiltaksgjennomforinger[2].sanityId ||
      window.crypto.randomUUID(),
    tiltakstype: "Mentor",
    tittel: "Oppfølging hos Muligheter AS",
  },
];
