import { DelMedBrukerDto } from "@api-client";
import { tiltakAmoGruppe, tiltakAvklaring } from "./mockGjennomforinger";

export const mockDeltMedBruker: DelMedBrukerDto[] = [
  {
    id: 1,
    sanityId: tiltakAvklaring.id,
    navIdent: "B123456",
    norskIdent: "11223344557",
    dialogId: "1",
    createdAt: new Date(2022, 2, 22).toString(),
    gjennomforingId: null,
    deltFraFylke: null,
    deltFraEnhet: null,
  },
  {
    id: 2,
    gjennomforingId: tiltakAmoGruppe.id,
    navIdent: "B123456",
    norskIdent: "11223344557",
    dialogId: "2",
    createdAt: new Date(2024, 0, 11).toString(),
    sanityId: null,
    deltFraFylke: null,
    deltFraEnhet: null,
  },
];
