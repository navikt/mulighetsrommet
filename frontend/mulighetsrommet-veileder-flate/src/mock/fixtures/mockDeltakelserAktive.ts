import {
  DataElementStatusVariant,
  Deltakelse,
  DeltakelseEierskap,
  DeltakelseTilstand,
  DeltakerStatusType,
  Tiltakskode,
} from "@api-client";
import { tiltakAft, tiltakAvklaring, tiltakJobbklubb } from "./mockGjennomforinger";

export const deltakelserAktive: Deltakelse[] = [
  {
    id: window.crypto.randomUUID(),
    innsoktDato: "2024-03-02",
    sistEndretDato: "2024-03-27",
    status: {
      type: { value: "Kladden", variant: DataElementStatusVariant.WARNING, description: null },
      aarsak: null,
    },
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    },
    tittel: "Arbeidsforberedende trening hos Barneverns- og Helsenemnda",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.KLADD,
    periode: { startDato: null, sluttDato: null },
    pamelding: {
      gjennomforingId: tiltakAft.id,
      status: DeltakerStatusType.KLADD,
    },
  },
  {
    id: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    sistEndretDato: "2024-03-27",
    status: {
      type: {
        value: "Utkast til påmelding",
        variant: DataElementStatusVariant.INFO,
        description: null,
      },
      aarsak: null,
    },
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Tiltakskode.AVKLARING,
    },
    tittel: "Avklaring hos Fretex AS",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.UTKAST,
    periode: { startDato: null, sluttDato: null },
    pamelding: {
      gjennomforingId: tiltakAvklaring.id,
      status: DeltakerStatusType.UTKAST_TIL_PAMELDING,
    },
  },
  {
    id: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: {
        value: "Venter på oppstart",
        variant: DataElementStatusVariant.ALT_3,
        description: null,
      },
      aarsak: null,
    },
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Tiltakskode.JOBBKLUBB,
    },
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
    sistEndretDato: null,
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.AKTIV,
    pamelding: {
      gjennomforingId: tiltakJobbklubb.id,
      status: DeltakerStatusType.VENTER_PA_OPPSTART,
    },
  },
  {
    id: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: { value: "Deltar", variant: DataElementStatusVariant.BLANK, description: null },
      aarsak: null,
    },
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Tiltakskode.JOBBKLUBB,
    },
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    sistEndretDato: null,
    tittel: "Jobbklubb hos Fretex",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.AKTIV,
    pamelding: {
      gjennomforingId: tiltakJobbklubb.id,
      status: DeltakerStatusType.DELTAR,
    },
  },
  {
    id: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: { value: "Gjennomføres", variant: DataElementStatusVariant.BLANK, description: null },
      aarsak: null,
    },
    tiltakstype: {
      navn: "Arbeidstrening",
      tiltakskode: null,
    },
    periode: {
      startDato: "2023-08-10",
      sluttDato: null,
    },
    tittel: "Arbeidstrening hos Fretex",
    sistEndretDato: null,
    eierskap: DeltakelseEierskap.TEAM_TILTAK,
    tilstand: DeltakelseTilstand.AKTIV,
    pamelding: null,
  },
];
