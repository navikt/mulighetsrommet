import {
  ArbeidsgiverAvtaleStatus,
  Deltakelse,
  DeltakelseEierskap,
  GruppetiltakDeltakerStatus,
} from "@mr/api-client-v2";
import { tiltakAft, tiltakAvklaring, tiltakJobbklubb } from "./mockTiltaksgjennomforinger";

export const deltakelserAktive: Deltakelse[] = [
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAft.id,
    innsoktDato: "2024-03-02",
    sistEndretDato: "2024-03-27",
    status: {
      type: GruppetiltakDeltakerStatus.KLADD,
      visningstekst: "Kladden er ikke delt",
    },
    tiltakstypeNavn: "Arbeidsforberende trening",
    tittel: "Arbeidsforberedende trening hos Barneverns- og Helsenemnda",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    periode: {},
  },
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAvklaring.id,
    innsoktDato: "2024-02-01",
    sistEndretDato: "2024-03-27",
    status: {
      type: GruppetiltakDeltakerStatus.UTKAST_TIL_PAMELDING,
      visningstekst: "Utkastet er delt og venter på godkjenning",
    },
    tiltakstypeNavn: "Avklaring",
    tittel: "Avklaring hos Fretex AS",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    periode: {},
  },
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakJobbklubb.id,
    innsoktDato: "2024-02-01",
    status: {
      type: GruppetiltakDeltakerStatus.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstypeNavn: "Jobbklubb",
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
  },
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakJobbklubb.id,
    innsoktDato: "2024-02-01",
    status: {
      type: GruppetiltakDeltakerStatus.DELTAR,
      visningstekst: "Deltar",
    },
    tiltakstypeNavn: "Jobbklubb",
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
  },
  {
    id: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: ArbeidsgiverAvtaleStatus.GJENNOMFORES,
      visningstekst: "Venter på oppstart",
    },
    tiltakstypeNavn: "Arbeidstrening",
    periode: {
      startDato: "2023-08-10",
    },
    tittel: "Arbeidstrening hos Fretex",
    eierskap: DeltakelseEierskap.TEAM_TILTAK,
  },
];
