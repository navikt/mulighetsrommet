import { TiltakDeltMedBruker, Tiltakskode } from "@api-client";

export const mockHistorikkDeltMedBruker: TiltakDeltMedBruker[] = [
  {
    navn: "Jobbklubb",
    tiltakstype: {
      tiltakskode: Tiltakskode.JOBBKLUBB,
      arenakode: null,
      navn: "Jobbklubb",
    },
    dialogId: "1",
    tiltakId: "1",
    createdAt: "2024-05-14",
  },
  {
    navn: "Jobbklubb",
    tiltakstype: {
      tiltakskode: Tiltakskode.JOBBKLUBB,
      arenakode: null,
      navn: "Jobbklubb",
    },
    dialogId: "2",
    tiltakId: "1",
    createdAt: "2023-05-14",
  },
  {
    navn: "Jobbklubb",
    tiltakstype: {
      tiltakskode: Tiltakskode.JOBBKLUBB,
      arenakode: null,
      navn: "Jobbklubb",
    },
    dialogId: "3",
    tiltakId: "1",
    createdAt: "2023-12-12",
  },
  {
    navn: "Avklaring",
    tiltakstype: {
      tiltakskode: Tiltakskode.AVKLARING,
      arenakode: null,
      navn: "Avklaring",
    },
    dialogId: "4",
    tiltakId: "2",
    createdAt: "2024-02-10",
  },
  {
    navn: "Avklaring",
    tiltakstype: {
      tiltakskode: Tiltakskode.AVKLARING,
      arenakode: null,
      navn: "Avklaring",
    },
    dialogId: "5",
    tiltakId: "2",
    createdAt: "2024-01-05",
  },
  {
    navn: "Mentor",
    tiltakstype: {
      tiltakskode: null,
      arenakode: "MENTOR",
      navn: "Mentor",
    },
    dialogId: "6",
    tiltakId: "3",
    createdAt: "2018-10-12",
  },
];
