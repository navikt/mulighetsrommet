import {
  DataElementStatusVariant,
  Deltakelse,
  DeltakelseTilstand,
  DeltakerStatusType,
  DeltakelseTiltaksadministrasjonDeltakelseInfoMeldingStatus,
  Tiltakskode,
} from "@arbeidsmarkedstiltak/api-client";
import {
  tiltakAft,
  tiltakAvklaring,
  tiltakFagOgYrke,
  tiltakJobbklubb,
} from "./mockGjennomforinger";

export const deltakelserAktive: Deltakelse[] = [
  {
    type: "TILTAKSADMINISTRASJON",
    id: window.crypto.randomUUID(),
    innsoktDato: "2024-03-02",
    sistEndretDato: "2024-03-27",
    status: {
      type: { value: "Kladden", variant: DataElementStatusVariant.WARNING, description: null },
      aarsak: null,
    },
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    tittel: "Arbeidsforberedende trening hos Barneverns- og Helsenemnda",
    tilstand: DeltakelseTilstand.KLADD,
    periode: { startDato: null, sluttDato: null },
    gjennomforingId: tiltakAft.id,
    infoMeldingStatus: DeltakelseTiltaksadministrasjonDeltakelseInfoMeldingStatus.KLADD,
  },
  {
    type: "TILTAKSADMINISTRASJON",
    id: window.crypto.randomUUID(),
    innsoktDato: "2026-03-02",
    sistEndretDato: "2026-03-27",
    status: {
      type: { value: "Kladden", variant: DataElementStatusVariant.WARNING, description: null },
      aarsak: null,
    },
    tiltakstype: {
      navn: "Arbeidsmarkedsopplæring (AMO)",
      tiltakskode: Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
    },
    tittel: "Arbeidsforberedende trening hos Barneverns- og Helsenemnda",
    tilstand: DeltakelseTilstand.KLADD,
    periode: { startDato: null, sluttDato: null },
    gjennomforingId: tiltakFagOgYrke.sanityId,
    infoMeldingStatus: DeltakelseTiltaksadministrasjonDeltakelseInfoMeldingStatus.SOKT_INN,
  },
  {
    type: "TILTAKSADMINISTRASJON",
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
    },
    tittel: "Avklaring hos Fretex AS",
    tilstand: DeltakelseTilstand.UTKAST,
    periode: { startDato: null, sluttDato: null },
    gjennomforingId: tiltakAvklaring.id,
    infoMeldingStatus:
      DeltakelseTiltaksadministrasjonDeltakelseInfoMeldingStatus.UTKAST_TIL_PAMELDING,
  },
  {
    type: "TILTAKSADMINISTRASJON",
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
    },
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
    sistEndretDato: null,
    tilstand: DeltakelseTilstand.AKTIV,
    gjennomforingId: tiltakJobbklubb.id,
    infoMeldingStatus:
      DeltakelseTiltaksadministrasjonDeltakelseInfoMeldingStatus.VENTER_PA_OPPSTART,
  },
  {
    type: "TILTAKSADMINISTRASJON",
    id: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: { value: "Deltar", variant: DataElementStatusVariant.BLANK, description: null },
      aarsak: null,
    },
    tiltakstype: {
      navn: "Jobbklubb",
    },
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    sistEndretDato: null,
    tittel: "Jobbklubb hos Fretex",
    tilstand: DeltakelseTilstand.AKTIV,
    gjennomforingId: tiltakJobbklubb.id,
    infoMeldingStatus: DeltakelseTiltaksadministrasjonDeltakelseInfoMeldingStatus.DELTAR,
  },
  {
    type: "TILTAK_ARBEIDSGIVER",
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Gjennomføres", variant: DataElementStatusVariant.BLANK, description: null },
      aarsak: null,
    },
    tiltakstype: {
      navn: "Arbeidstrening",
    },
    periode: {
      startDato: "2023-08-10",
      sluttDato: null,
    },
    tittel: "Arbeidstrening hos Fretex",
    tilstand: DeltakelseTilstand.AKTIV,
  },
];
