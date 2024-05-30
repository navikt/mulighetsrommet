import { DeltakerKort, DeltakerStatusType, TiltakskodeArena } from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "./mockTiltaksgjennomforinger";

export const utkastFraKomet: DeltakerKort[] = [
  {
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    innsoktDato: "2024-03-02",
    sistEndretDato: "2024-03-27",
    status: {
      type: DeltakerStatusType.KLADD,
      visningstekst: "Kladden er ikke delt",
    },
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: TiltakskodeArena.AVKLARAG,
    },
    tittel: "Avklaring hos Muligheter AS",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    sistEndretDato: "2024-03-27",
    status: {
      type: DeltakerStatusType.UTKAST_TIL_PAMELDING,
      visningstekst: "Utkastet er delt og venter på godkjenning",
    },
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: TiltakskodeArena.AVKLARAG,
    },
    tittel: "Avklaring hos Fretex AS",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: TiltakskodeArena.JOBBK,
    },
    tittel: "Jobbklubb hos Fretex",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: TiltakskodeArena.JOBBK,
    },
    periode: {
      startdato: "2023-08-10",
      sluttdato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.DELTAR,
      visningstekst: "Deltar",
    },
    tiltakstype: {
      navn: "Jobbklubb",
      tiltakskode: TiltakskodeArena.JOBBK,
    },
    periode: {
      startdato: "2023-08-10",
      sluttdato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
  },
  {
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
    },
    tiltakstype: {
      navn: "Varig tilrettelagt arbeid (VTA)",
      tiltakskode: TiltakskodeArena.VASV,
    },
    periode: {
      startdato: "2023-08-10",
    },
    tittel: "VTA hos Fretex",
  },
];
