import { DelMedBrukerDbo } from "@api-client";
import { tiltakAmoGruppe, tiltakAvklaring } from "./mockGjennomforinger";

export const mockDeltMedBruker: DelMedBrukerDbo[] = [
  {
    sanityId: tiltakAvklaring.id,
    navident: "B123456",
    norskIdent: "11223344557",
    dialogId: "1",
    createdAt: new Date(2022, 2, 22).toString(),
    id: null,
    gjennomforingId: null,
    updatedAt: null,
    createdBy: null,
    updatedBy: null,
    tiltakstypeNavn: null,
    deltFraFylke: null,
    deltFraEnhet: null,
  },
  {
    gjennomforingId: tiltakAmoGruppe.id,
    navident: "B123456",
    norskIdent: "11223344557",
    dialogId: "2",
    createdAt: new Date(2024, 0, 11).toString(),
    id: null,
    sanityId: null,
    updatedAt: null,
    createdBy: null,
    updatedBy: null,
    tiltakstypeNavn: null,
    deltFraFylke: null,
    deltFraEnhet: null,
  },
];
