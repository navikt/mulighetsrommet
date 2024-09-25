import { AmtDeltakerStatusType, DeltakerKort, DeltakerKortEierskap } from "@mr/api-client";
import { tiltakAvklaring, tiltakOppfolging } from "./mockTiltaksgjennomforinger";

export const deltakelserHistoriske: DeltakerKort[] = [
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstypeNavn: "Oppfølging",
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakOppfolging.id,
    status: {
      type: AmtDeltakerStatusType.AVBRUTT_UTKAST,
      visningstekst: "Avbrutt utkast",
    },
    sistEndretDato: "2024-03-07",
    innsoktDato: "2024-03-02",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
    periode: {},
  },
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstypeNavn: "Oppfølging",

    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakOppfolging.id,
    status: {
      type: AmtDeltakerStatusType.IKKE_AKTUELL,
      visningstekst: "Ikke aktuell",
      aarsak: "utdanning",
    },
    innsoktDato: "2024-03-02",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
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
    gjennomforingId: tiltakOppfolging.id,
    status: {
      type: AmtDeltakerStatusType.HAR_SLUTTET,
      visningstekst: "Har sluttet",
    },
    innsoktDato: "2024-03-02",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
  },
  {
    periode: {
      startDato: "2023-01.01",
      sluttDato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstypeNavn: "Avklaring",

    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAvklaring.id,
    status: {
      type: AmtDeltakerStatusType.FULLFORT,
      visningstekst: "Fullført",
    },
    innsoktDato: "2024-02-03",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
  },
  {
    periode: {
      startDato: "2023-01.01",
      sluttDato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstypeNavn: "Avklaring",

    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAvklaring.id,
    status: {
      type: AmtDeltakerStatusType.AVBRUTT,
      visningstekst: "Avbrutt",
      aarsak: "Fått jobb",
    },
    innsoktDato: "2024-02-03",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
  },
  {
    tittel: "Avklaring med Anne",
    tiltakstypeNavn: "Avklaring",

    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAvklaring.id,
    status: {
      type: AmtDeltakerStatusType.FEILREGISTRERT,
      visningstekst: "Feilregistrert",
    },
    innsoktDato: "2024-02-03",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
    periode: {},
  },
];
