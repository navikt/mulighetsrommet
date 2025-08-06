import { DeltMedBrukerDto } from "@api-client";
import { tiltakAmoGruppe, tiltakAvklaring } from "./mockGjennomforinger";

export const mockDeltMedBruker: DeltMedBrukerDto[] = [
  {
    tiltakId: tiltakAvklaring.id,
    deling: {
      dialogId: "1",
      tidspunkt: new Date(2022, 2, 22).toString(),
    },
  },
  {
    tiltakId: tiltakAmoGruppe.id,
    deling: {
      dialogId: "2",
      tidspunkt: new Date(2024, 0, 11).toString(),
    },
  },
];
