import { DelMedBrukerDto, Tiltakskode } from "@arbeidsmarkedstiltak/api-client";
import {
  tiltakAvklaring,
  tiltakJobbklubb,
  tiltakMentor,
} from "@/mock/fixtures/mockGjennomforinger";

export const mockHistorikkDeltMedBruker: DelMedBrukerDto[] = [
  {
    tiltak: {
      id: tiltakJobbklubb.id,
      navn: "Jobbklubb",
      slettet: false,
    },
    dialogId: "1",
    tidspunkt: "2024-05-14",
    tiltakstype: {
      tiltakskode: Tiltakskode.JOBBKLUBB,
      navn: "Jobbklubb",
    },
  },
  {
    tiltak: {
      id: tiltakJobbklubb.id,
      navn: "Jobbklubb",
      slettet: false,
    },
    dialogId: "2",
    tidspunkt: "2023-05-14",
    tiltakstype: {
      tiltakskode: Tiltakskode.JOBBKLUBB,
      navn: "Jobbklubb",
    },
  },
  {
    tiltak: {
      id: tiltakJobbklubb.id,
      navn: "Jobbklubb",
      slettet: false,
    },
    dialogId: "3",
    tidspunkt: "2023-12-12",
    tiltakstype: {
      tiltakskode: Tiltakskode.JOBBKLUBB,
      navn: "Jobbklubb",
    },
  },
  {
    tiltak: {
      id: tiltakAvklaring.id,
      navn: "Avklaring",
      slettet: false,
    },
    dialogId: "4",
    tidspunkt: "2024-02-10",
    tiltakstype: {
      tiltakskode: Tiltakskode.AVKLARING,
      navn: "Avklaring",
    },
  },
  {
    tiltak: {
      id: tiltakAvklaring.id,
      navn: "Avklaring",
      slettet: false,
    },
    dialogId: "5",
    tidspunkt: "2024-01-05",
    tiltakstype: {
      tiltakskode: Tiltakskode.AVKLARING,
      navn: "Avklaring",
    },
  },
  {
    tiltak: {
      id: tiltakMentor.id,
      navn: "Mentor",
      slettet: true,
    },
    dialogId: "6",
    tidspunkt: "2018-10-12",
    tiltakstype: {
      tiltakskode: Tiltakskode.MENTOR,
      navn: "Mentor",
    },
  },
];
