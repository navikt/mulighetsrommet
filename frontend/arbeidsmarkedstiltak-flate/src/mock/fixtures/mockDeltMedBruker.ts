import { DelMedBrukerDto, Tiltakskode } from "@arbeidsmarkedstiltak/api-client";
import { tiltakAmoGruppe, tiltakAvklaring } from "./mockGjennomforinger";

export const mockDeltMedBruker: DelMedBrukerDto[] = [
  {
    tiltak: {
      id: tiltakAvklaring.id,
      navn: "Aft",
      slettet: false,
    },
    tiltakstype: {
      tiltakskode: Tiltakskode.ARBEID_MED_STOTTE,
      navn: "AFT",
    },
    dialogId: "1",
    tidspunkt: new Date(2022, 2, 22).toString(),
  },
  {
    tiltak: {
      id: tiltakAmoGruppe.id,
      navn: "Aft",
      slettet: false,
    },
    dialogId: "2",
    tidspunkt: new Date(2024, 0, 11).toString(),
    tiltakstype: {
      tiltakskode: Tiltakskode.ARBEID_MED_STOTTE,
      navn: "AFT",
    },
  },
];
