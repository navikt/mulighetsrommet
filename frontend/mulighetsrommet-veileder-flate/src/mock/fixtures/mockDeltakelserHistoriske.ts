import {
  GruppetiltakDeltakerStatus,
  Deltakelse,
  DeltakelseEierskap,
  ArbeidsgiverAvtaleStatus,
  EierskapEnum,
} from "@mr/api-client-v2";
import { tiltakAvklaring, tiltakOppfolging } from "./mockGjennomforinger";

export const deltakelserHistoriske: Deltakelse[] = [
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstypeNavn: "Oppfølging",
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakOppfolging.id,
    status: {
      type: GruppetiltakDeltakerStatus.AVBRUTT_UTKAST,
      visningstekst: "Avbrutt utkast",
    },
    sistEndretDato: "2024-03-07",
    innsoktDato: "2024-03-02",
    eierskap: EierskapEnum.TEAM_KOMET,
    periode: {},
  },
  {
    tittel: "Mentor hos Fretex AS",
    tiltakstypeNavn: "Mentor",
    id: window.crypto.randomUUID(),
    status: {
      type: ArbeidsgiverAvtaleStatus.AVSLUTTET,
      visningstekst: "Avsluttet",
    },
    sistEndretDato: "2024-03-07",
    innsoktDato: "2024-03-02",
    eierskap: DeltakelseEierskap.TEAM_TILTAK,
    periode: {},
  },
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstypeNavn: "Oppfølging",

    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakOppfolging.id,
    status: {
      type: GruppetiltakDeltakerStatus.IKKE_AKTUELL,
      visningstekst: "Ikke aktuell",
      aarsak: "utdanning",
    },
    innsoktDato: "2024-03-02",
    eierskap: EierskapEnum.TEAM_KOMET,
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
      type: GruppetiltakDeltakerStatus.HAR_SLUTTET,
      visningstekst: "Har sluttet",
    },
    innsoktDato: "2024-03-02",
    eierskap: EierskapEnum.TEAM_KOMET,
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
      type: GruppetiltakDeltakerStatus.FULLFORT,
      visningstekst: "Fullført",
    },
    innsoktDato: "2024-02-03",
    eierskap: EierskapEnum.TEAM_KOMET,
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
      type: GruppetiltakDeltakerStatus.AVBRUTT,
      visningstekst: "Avbrutt",
      aarsak: "Fått jobb",
    },
    innsoktDato: "2024-02-03",
    eierskap: EierskapEnum.TEAM_KOMET,
  },
  {
    tittel: "Avklaring med Anne",
    tiltakstypeNavn: "Avklaring",

    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAvklaring.id,
    status: {
      type: GruppetiltakDeltakerStatus.FEILREGISTRERT,
      visningstekst: "Feilregistrert",
    },
    innsoktDato: "2024-02-03",
    eierskap: EierskapEnum.TEAM_KOMET,
    periode: {},
  },
  {
    tittel: "Gammel Avklaring med Anne",
    tiltakstypeNavn: "Avklaring",
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAvklaring.id,
    status: {
      type: GruppetiltakDeltakerStatus.FULLFORT,
      visningstekst: "Fullført",
    },
    innsoktDato: "2017-02-03",
    eierskap: EierskapEnum.TEAM_KOMET,
    periode: {},
  },
];
