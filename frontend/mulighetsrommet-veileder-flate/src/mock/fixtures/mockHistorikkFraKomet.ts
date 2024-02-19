import { HistorikkForBrukerFraKomet } from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "./mockTiltaksgjennomforinger";

export const historikkFraKomet: HistorikkForBrukerFraKomet[] = [
  {
    fnr: "12345678910",
    fraDato: "10.05.2023",
    tilDato: "12.12.2023",
    arsak: null,
    tiltaksnavn: "Oppfølging hos Muligheter AS",
    tiltakstype: "Oppfølging",
    id: window.crypto.randomUUID(),
    status: HistorikkForBrukerFraKomet.status.AVSLUTTET,
    tiltaksgjennomforingId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
  },

  {
    fnr: "12345678910",
    fraDato: "01.01.2024",
    tilDato: "01.06.2025",
    arsak: null,
    tiltaksnavn: "Arbeidstrening hos Jobbfrukt ASA",
    tiltakstype: "Arbeidstrening",
    id: window.crypto.randomUUID(),
    status: HistorikkForBrukerFraKomet.status.DELTAR,
    tiltaksgjennomforingId:
      mockTiltaksgjennomforinger[1].id ||
      mockTiltaksgjennomforinger[1].sanityId ||
      window.crypto.randomUUID(),
  },
  {
    fnr: "12345678910",
    fraDato: "01.01.2024",
    tilDato: "01.02.2024",
    tiltaksnavn: "Mentor med Manfred",
    tiltakstype: "Mentor",
    id: window.crypto.randomUUID(),
    status: HistorikkForBrukerFraKomet.status.AVSLUTTET,
    tiltaksgjennomforingId:
      mockTiltaksgjennomforinger[2].id ||
      mockTiltaksgjennomforinger[2].sanityId ||
      window.crypto.randomUUID(),
    arsak: "Dårlig stemning mellom mentor og bruker",
  },
];
