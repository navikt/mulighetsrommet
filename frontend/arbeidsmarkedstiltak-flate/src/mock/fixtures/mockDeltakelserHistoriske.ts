import {
  DataElementStatusVariant,
  Deltakelse,
  DeltakelseTilstand,
  DeltakerStatusType,
  Tiltakskode,
} from "@arbeidsmarkedstiltak/api-client";
import { tiltakAvklaring, tiltakOppfolging } from "./mockGjennomforinger";

export const deltakelserHistoriske: Deltakelse[] = [
  {
    type: "TILTAKSADMINISTRASJON",
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
    },
    id: window.crypto.randomUUID(),
    status: {
      type: {
        value: "Avbrutt utkast",
        variant: DataElementStatusVariant.NEUTRAL,
        description: null,
      },
      aarsak: null,
    },
    sistEndretDato: "2024-03-07",
    innsoktDato: "2024-03-02",
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
    gjennomforingId: tiltakOppfolging.id,
    infoMeldingStatus: null,
  },
  {
    type: "TILTAK_ARBEIDSGIVER",
    tittel: "Mentor hos Fretex AS",
    tiltakstype: {
      navn: "Mentor",
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Avsluttet", variant: DataElementStatusVariant.ALT_1, description: null },
      aarsak: null,
    },
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
  },
  {
    type: "TILTAKSADMINISTRASJON",
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Ikke aktuell", variant: DataElementStatusVariant.NEUTRAL, description: null },
      aarsak: "utdanning",
    },
    sistEndretDato: null,
    innsoktDato: "2024-03-02",
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
    gjennomforingId: tiltakOppfolging.id,
    infoMeldingStatus: null,
  },
  {
    type: "TILTAKSADMINISTRASJON",
    periode: {
      startDato: "2023-05-10",
      sluttDato: "2023-12-12",
    },
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Har sluttet", variant: DataElementStatusVariant.ALT_1, description: null },
      aarsak: null,
    },
    sistEndretDato: null,
    innsoktDato: "2024-03-02",
    tilstand: DeltakelseTilstand.AVSLUTTET,
    gjennomforingId: tiltakOppfolging.id,
    infoMeldingStatus: null,
  },
  {
    type: "TILTAKSADMINISTRASJON",
    periode: {
      startDato: "2023-01.01",
      sluttDato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Fullført", variant: DataElementStatusVariant.ALT_1, description: null },
      aarsak: null,
    },
    sistEndretDato: null,
    innsoktDato: "2024-02-03",
    tilstand: DeltakelseTilstand.AVSLUTTET,
    gjennomforingId: tiltakAvklaring.id,
    infoMeldingStatus: null,
  },
  {
    type: "TILTAKSADMINISTRASJON",
    periode: {
      startDato: "2023-01.01",
      sluttDato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Avbrutt", variant: DataElementStatusVariant.NEUTRAL, description: null },
      aarsak: "Fått jobb",
    },
    sistEndretDato: null,
    innsoktDato: "2024-02-03",
    tilstand: DeltakelseTilstand.AVSLUTTET,
    gjennomforingId: tiltakAvklaring.id,
    infoMeldingStatus: null,
  },
  {
    type: "TILTAKSADMINISTRASJON",
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
    },
    id: window.crypto.randomUUID(),
    status: {
      type: {
        value: "Feilregistrert",
        variant: DataElementStatusVariant.NEUTRAL,
        description: null,
      },
      aarsak: null,
    },
    sistEndretDato: null,
    innsoktDato: "2024-02-03",
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
    gjennomforingId: tiltakAvklaring.id,
    infoMeldingStatus: null,
  },
  {
    type: "TILTAKSADMINISTRASJON",
    tittel: "Gammel Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Fullført", variant: DataElementStatusVariant.ALT_1, description: null },
      aarsak: null,
    },
    sistEndretDato: null,
    innsoktDato: "2017-02-03",
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
    gjennomforingId: tiltakAvklaring.id,
    infoMeldingStatus: null,
  },
];
