import { AmtDeltakerStatusType, DeltakerKort, DeltakerKortEierskap } from "@mr/api-client";
import {
  tiltakAft,
  tiltakAvklaring,
  tiltakJobbklubb,
  tiltakVta,
} from "./mockTiltaksgjennomforinger";

export const deltakelserAktive: DeltakerKort[] = [
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAft.id,
    innsoktDato: "2024-03-02",
    sistEndretDato: "2024-03-27",
    status: {
      type: AmtDeltakerStatusType.KLADD,
      visningstekst: "Kladden er ikke delt",
    },
    tiltakstypeNavn: "Arbeidsforberende trening",
    tittel: "Arbeidsforberedende trening hos Barneverns- og Helsenemnda",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
    periode: {},
  },
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAvklaring.id,
    innsoktDato: "2024-02-01",
    sistEndretDato: "2024-03-27",
    status: {
      type: AmtDeltakerStatusType.UTKAST_TIL_PAMELDING,
      visningstekst: "Utkastet er delt og venter på godkjenning",
    },
    tiltakstypeNavn: "Avklaring",
    tittel: "Avklaring hos Fretex AS",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
    periode: {},
  },
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakJobbklubb.id,
    innsoktDato: "2024-02-01",
    status: {
      type: AmtDeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstypeNavn: "Jobbklubb",
    tittel: "Jobbklubb hos Fretex",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
    periode: {},
  },
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakJobbklubb.id,
    innsoktDato: "2024-02-01",
    status: {
      type: AmtDeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstypeNavn: "Jobbklubb",
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
  },
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakJobbklubb.id,
    innsoktDato: "2024-02-01",
    status: {
      type: AmtDeltakerStatusType.DELTAR,
      visningstekst: "Deltar",
    },
    tiltakstypeNavn: "Jobbklubb",
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
  },
  {
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakVta.id,
    innsoktDato: "2024-02-01",
    status: {
      type: AmtDeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstypeNavn: "Varig tilrettelagt arbeid (VTA)",
    periode: {
      startDato: "2023-08-10",
    },
    tittel: "VTA hos Fretex",
    eierskap: DeltakerKortEierskap.TEAM_KOMET,
  },
];
