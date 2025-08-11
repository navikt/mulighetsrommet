import { TiltakDeltMedBrukerDto, Tiltakskode } from "@api-client";
import {
  tiltakAvklaring,
  tiltakJobbklubb,
  tiltakMentor,
} from "@/mock/fixtures/mockGjennomforinger";

export const mockHistorikkDeltMedBruker: TiltakDeltMedBrukerDto[] = [
  {
    tiltak: {
      id: tiltakJobbklubb.id,
      navn: "Jobbklubb",
    },
    deling: {
      dialogId: "1",
      tidspunkt: "2024-05-14",
    },
    tiltakstype: {
      tiltakskode: Tiltakskode.JOBBKLUBB,
      arenakode: "JOBBK",
      navn: "Jobbklubb",
    },
  },
  {
    tiltak: {
      id: tiltakJobbklubb.id,
      navn: "Jobbklubb",
    },
    deling: {
      dialogId: "2",
      tidspunkt: "2023-05-14",
    },
    tiltakstype: {
      tiltakskode: Tiltakskode.JOBBKLUBB,
      arenakode: null,
      navn: "Jobbklubb",
    },
  },
  {
    tiltak: {
      id: tiltakJobbklubb.id,
      navn: "Jobbklubb",
    },
    deling: {
      dialogId: "3",
      tidspunkt: "2023-12-12",
    },
    tiltakstype: {
      tiltakskode: Tiltakskode.JOBBKLUBB,
      arenakode: null,
      navn: "Jobbklubb",
    },
  },
  {
    tiltak: {
      id: tiltakAvklaring.id,
      navn: "Avklaring",
    },
    deling: {
      dialogId: "4",
      tidspunkt: "2024-02-10",
    },
    tiltakstype: {
      tiltakskode: Tiltakskode.AVKLARING,
      arenakode: null,
      navn: "Avklaring",
    },
  },
  {
    tiltak: {
      id: tiltakAvklaring.id,
      navn: "Avklaring",
    },
    deling: {
      dialogId: "5",
      tidspunkt: "2024-01-05",
    },
    tiltakstype: {
      tiltakskode: Tiltakskode.AVKLARING,
      arenakode: null,
      navn: "Avklaring",
    },
  },
  {
    tiltak: {
      id: tiltakMentor.sanityId,
      navn: "Mentor",
    },
    deling: {
      dialogId: "6",
      tidspunkt: "2018-10-12",
    },
    tiltakstype: {
      tiltakskode: null,
      arenakode: "MENTOR",
      navn: "Mentor",
    },
  },
];
