import { DeltakerKort, DeltakerStatus, Gruppetiltak } from "mulighetsrommet-api-client";

export const utkastFraKomet: DeltakerKort[] = [
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-03-02",
    sistEndretDato: "2024-03-27",
    status: {
      type: DeltakerStatus.type.KLADD,
      visningstekst: "Kladden er ikke delt",
    },
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    tittel: "Avklaring hos Muligheter AS",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    sistEndretDato: "2024-03-27",
    status: {
      type: DeltakerStatus.type.UTKAST_TIL_PAMELDING,
      visningstekst: "Utkastet er delt og venter på godkjenning",
    },
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    tittel: "Avklaring hos Fretex AS",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatus.type.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Gruppetiltak.tiltakskode.JOBBK,
    },
    tittel: "Jobbklubb hos Fretex",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatus.type.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Gruppetiltak.tiltakskode.JOBBK,
    },
    periode: {
      startdato: "2023-08-10",
      sluttdato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatus.type.DELTAR,
      visningstekst: "Deltar",
    },
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: Gruppetiltak.tiltakskode.JOBBK,
    },
    periode: {
      startdato: "2023-08-10",
      sluttdato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatus.type.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstype: {
      navn: "Varig tilrettelagt arbeid (VTA)",
      tiltakskode: Gruppetiltak.tiltakskode.VASV,
    },
    periode: {
      startdato: "2023-08-10",
    },
    tittel: "VTA hos Fretex",
  },
];

export const historikkFraKomet: DeltakerKort[] = [
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Gruppetiltak.tiltakskode.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    status: {
      type: DeltakerStatus.type.AVBRUTT_UTKAST,
      visningstekst: "Avbrutt utkast",
    },
    sistEndretDato: "2024-03-07",
    innsoktDato: "2024-03-02",
  },
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Gruppetiltak.tiltakskode.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    status: {
      type: DeltakerStatus.type.IKKE_AKTUELL,
      visningstekst: "Ikke aktuell",
      aarsak: "utdanning",
    },
    innsoktDato: "2024-03-02",
  },
  {
    periode: {
      startdato: "2023-05-10",
      sluttdato: "2023-12-12",
    },
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: Gruppetiltak.tiltakskode.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    status: {
      type: DeltakerStatus.type.HAR_SLUTTET,
      visningstekst: "Har sluttet",
    },
    innsoktDato: "2024-03-02",
  },
  {
    periode: {
      startdato: "2023-01.01",
      sluttdato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    deltakerId: window.crypto.randomUUID(),
    status: {
      type: DeltakerStatus.type.FULLFORT,
      visningstekst: "Fullført",
    },
    innsoktDato: "2024-02-03",
  },
  {
    periode: {
      startdato: "2023-01.01",
      sluttdato: "2024-02-01",
    },
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Gruppetiltak.tiltakskode.AVKLARAG,
    },
    deltakerId: window.crypto.randomUUID(),
    status: {
      type: DeltakerStatus.type.AVBRUTT,
      visningstekst: "Avbrutt",
      aarsak: "Fått jobb",
    },
    innsoktDato: "2024-02-03",
  },
];
