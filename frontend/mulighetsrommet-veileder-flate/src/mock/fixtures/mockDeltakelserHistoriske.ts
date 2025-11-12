import {
  DataElementStatusVariant,
  Deltakelse,
  DeltakelseEierskap,
  DeltakelseTilstand,
  DeltakerStatusType,
  Tiltakskode,
} from "@api-client";
import { tiltakAvklaring, tiltakOppfolging } from "./mockGjennomforinger";

export const deltakelserHistoriske: Deltakelse[] = [
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Tiltakskode.OPPFOLGING,
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
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
    pamelding: {
      gjennomforingId: tiltakOppfolging.id,
      status: DeltakerStatusType.AVBRUTT_UTKAST,
    },
  },
  {
    tittel: "Mentor hos Fretex AS",
    tiltakstype: {
      navn: "Mentor",
      tiltakskode: null,
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Avsluttet", variant: DataElementStatusVariant.ALT_1, description: null },
      aarsak: null,
    },
    sistEndretDato: "2024-03-07",
    innsoktDato: "2024-03-02",
    eierskap: DeltakelseEierskap.TEAM_TILTAK,
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
    pamelding: null,
  },
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Tiltakskode.OPPFOLGING,
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Ikke aktuell", variant: DataElementStatusVariant.NEUTRAL, description: null },
      aarsak: "utdanning",
    },
    sistEndretDato: null,
    innsoktDato: "2024-03-02",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
    pamelding: { gjennomforingId: tiltakOppfolging.id, status: DeltakerStatusType.IKKE_AKTUELL },
  },
  {
    periode: {
      startDato: "2023-05-10",
      sluttDato: "2023-12-12",
    },
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Tiltakskode.OPPFOLGING,
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Har sluttet", variant: DataElementStatusVariant.ALT_1, description: null },
      aarsak: null,
    },
    sistEndretDato: null,
    innsoktDato: "2024-03-02",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.AVSLUTTET,
    pamelding: {
      gjennomforingId: tiltakOppfolging.id,
      status: DeltakerStatusType.HAR_SLUTTET,
    },
  },
  {
    periode: {
      startDato: "2023-01.01",
      sluttDato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Tiltakskode.AVKLARING,
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Fullført", variant: DataElementStatusVariant.ALT_1, description: null },
      aarsak: null,
    },
    sistEndretDato: null,
    innsoktDato: "2024-02-03",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.AVSLUTTET,
    pamelding: {
      gjennomforingId: tiltakAvklaring.id,
      status: DeltakerStatusType.FULLFORT,
    },
  },
  {
    periode: {
      startDato: "2023-01.01",
      sluttDato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Tiltakskode.AVKLARING,
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Avbrutt", variant: DataElementStatusVariant.NEUTRAL, description: null },
      aarsak: "Fått jobb",
    },
    sistEndretDato: null,
    innsoktDato: "2024-02-03",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.AVSLUTTET,
    pamelding: {
      gjennomforingId: tiltakAvklaring.id,
      status: DeltakerStatusType.AVBRUTT,
    },
  },
  {
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Tiltakskode.AVKLARING,
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
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
    pamelding: {
      gjennomforingId: tiltakAvklaring.id,
      status: DeltakerStatusType.FEILREGISTRERT,
    },
  },
  {
    tittel: "Gammel Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Tiltakskode.AVKLARING,
    },
    id: window.crypto.randomUUID(),
    status: {
      type: { value: "Fullført", variant: DataElementStatusVariant.ALT_1, description: null },
      aarsak: null,
    },
    sistEndretDato: null,
    innsoktDato: "2017-02-03",
    eierskap: DeltakelseEierskap.TEAM_KOMET,
    tilstand: DeltakelseTilstand.AVSLUTTET,
    periode: { startDato: null, sluttDato: null },
    pamelding: {
      gjennomforingId: tiltakAvklaring.id,
      status: DeltakerStatusType.FULLFORT,
    },
  },
];
