import { DeltakerKort, DeltakerStatusType, TiltakskodeArena } from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "./mockTiltaksgjennomforinger";

export const historikkFraKomet: DeltakerKort[] = [
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: TiltakskodeArena.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    status: {
      type: DeltakerStatusType.AVBRUTT_UTKAST,
      visningstekst: "Avbrutt utkast",
    },
    sistEndretDato: "2024-03-07",
    innsoktDato: "2024-03-02",
  },
  {
    tittel: "Oppfølging hos Muligheter AS",
    tiltakstype: {
      navn: "Oppfølging",
      tiltakskode: TiltakskodeArena.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    status: {
      type: DeltakerStatusType.IKKE_AKTUELL,
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
      tiltakskode: TiltakskodeArena.INDOPPFAG,
    },
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    status: {
      type: DeltakerStatusType.HAR_SLUTTET,
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
      tiltakskode: TiltakskodeArena.AVKLARAG,
    },
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    status: {
      type: DeltakerStatusType.FULLFORT,
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
      tiltakskode: TiltakskodeArena.AVKLARAG,
    },
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    status: {
      type: DeltakerStatusType.AVBRUTT,
      visningstekst: "Avbrutt",
      aarsak: "Fått jobb",
    },
    innsoktDato: "2024-02-03",
  },
  {
    tittel: "Avklaring med Anne",
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: TiltakskodeArena.AVKLARAG,
    },
    deltakerId: window.crypto.randomUUID(),
    deltakerlisteId:
      mockTiltaksgjennomforinger[0].id ||
      mockTiltaksgjennomforinger[0].sanityId ||
      window.crypto.randomUUID(),
    status: {
      type: DeltakerStatusType.FEILREGISTRERT,
      visningstekst: "Feilregistrert",
    },
    innsoktDato: "2024-02-03",
  },
];
