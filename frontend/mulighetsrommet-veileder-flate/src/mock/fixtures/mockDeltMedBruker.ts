import { DelMedBrukerDto } from "@api-client";
import { tiltakAmoGruppe, tiltakAvklaring } from "./mockGjennomforinger";

export const mockDeltMedBruker: DelMedBrukerDto[] = [
  {
    id: 1,
    dialogId: "1",
    createdAt: new Date(2022, 2, 22).toString(),
    sanityId: tiltakAvklaring.id,
    gjennomforingId: null,
  },
  {
    id: 2,
    dialogId: "2",
    createdAt: new Date(2024, 0, 11).toString(),
    sanityId: null,
    gjennomforingId: tiltakAmoGruppe.id,
  },
];
