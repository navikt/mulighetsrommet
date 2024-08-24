import { DeltakerKort, DeltakerStatusType } from "@mr/api-client";
import { mockTiltaksgjennomforinger } from "./mockTiltaksgjennomforinger";

export const utkastFraKomet: DeltakerKort[] = [
  {
    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[0].id,
    innsoktDato: "2024-03-02",
    sistEndretDato: "2024-03-27",
    status: {
      type: DeltakerStatusType.KLADD,
      visningstekst: "Kladden er ikke delt",
    },
    tiltakstypeNavn: "Arbeidsforberende trening",
    tittel: "Arbeidsforberedende trening hos Barneverns- og Helsenemnda",
    eierskap: "KOMET",
  },
  {
    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[0].id,
    innsoktDato: "2024-02-01",
    sistEndretDato: "2024-03-27",
    status: {
      type: DeltakerStatusType.UTKAST_TIL_PAMELDING,
      visningstekst: "Utkastet er delt og venter på godkjenning",
    },
    tiltakstypeNavn: "Avklaring",
    tittel: "Avklaring hos Fretex AS",
    eierskap: "KOMET",
  },
  {
    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[0].id,
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstypeNavn: "Jobbklubb",
    tittel: "Jobbklubb hos Fretex",
    eierskap: "KOMET",
  },
  {
    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[0].id,
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstypeNavn: "Jobbklubb",
    periode: {
      startdato: "2023-08-10",
      sluttdato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
    eierskap: "KOMET",
  },
  {
    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[0].id,
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.DELTAR,
      visningstekst: "Deltar",
    },
    tiltakstypeNavn: "Jobbklubb",
    periode: {
      startdato: "2023-08-10",
      sluttdato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
    eierskap: "KOMET",
  },
  {
    id: window.crypto.randomUUID(),
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[0].id,
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstypeNavn: "Varig tilrettelagt arbeid (VTA)",
    periode: {
      startdato: "2023-08-10",
    },
    tittel: "VTA hos Fretex",
    eierskap: "KOMET",
  },
];
