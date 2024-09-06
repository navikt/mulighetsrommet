import { DeltakerKort, DeltakerStatusType } from "@mr/api-client";
import { tiltakAvklaring, tiltakOppfolging } from "./mockTiltaksgjennomforinger";

export const historikkFraKomet: DeltakerKort[] = [
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstypeNavn: "Oppfølging",
    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: tiltakOppfolging.id,
    status: {
      type: DeltakerStatusType.AVBRUTT_UTKAST,
      visningstekst: "Avbrutt utkast",
    },
    sistEndretDato: "2024-03-07",
    innsoktDato: "2024-03-02",
    eierskap: "KOMET",
    periode: {},
  },
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstypeNavn: "Oppfølging",

    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: tiltakOppfolging.id,
    status: {
      type: DeltakerStatusType.IKKE_AKTUELL,
      visningstekst: "Ikke aktuell",
      aarsak: "utdanning",
    },
    innsoktDato: "2024-03-02",
    eierskap: "KOMET",
    periode: {},
  },
  {
    periode: {
      startDato: "2023-05-10",
      sluttDato: "2023-12-12",
    },
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstypeNavn: "Oppfølging",

    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: tiltakOppfolging.id,
    status: {
      type: DeltakerStatusType.HAR_SLUTTET,
      visningstekst: "Har sluttet",
    },
    innsoktDato: "2024-03-02",
    eierskap: "KOMET",
  },
  {
    periode: {
      startDato: "2023-01.01",
      sluttDato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstypeNavn: "Avklaring",

    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: tiltakAvklaring.id,
    status: {
      type: DeltakerStatusType.FULLFORT,
      visningstekst: "Fullført",
    },
    innsoktDato: "2024-02-03",
    eierskap: "KOMET",
  },
  {
    periode: {
      startDato: "2023-01.01",
      sluttDato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstypeNavn: "Avklaring",

    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: tiltakAvklaring.id,
    status: {
      type: DeltakerStatusType.AVBRUTT,
      visningstekst: "Avbrutt",
      aarsak: "Fått jobb",
    },
    innsoktDato: "2024-02-03",
    eierskap: "KOMET",
  },
  {
    tittel: "Avklaring med Anne",
    tiltakstypeNavn: "Avklaring",

    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: tiltakAvklaring.id,
    status: {
      type: DeltakerStatusType.FEILREGISTRERT,
      visningstekst: "Feilregistrert",
    },
    innsoktDato: "2024-02-03",
    eierskap: "KOMET",
    periode: {},
  },
];
