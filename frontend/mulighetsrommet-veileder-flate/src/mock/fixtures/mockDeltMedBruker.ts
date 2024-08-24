import { DelMedBruker } from "@mr/api-client";
import { mockTiltaksgjennomforinger } from "./mockTiltaksgjennomforinger";

export const mockDeltMedBruker: DelMedBruker[] = [
  {
    sanityId: mockTiltaksgjennomforinger[0].id,
    navident: "B123456",
    norskIdent: "11223344557",
    dialogId: "1",
    createdAt: new Date(2022, 2, 22).toString(),
  },
  {
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[2].id,
    navident: "B123456",
    norskIdent: "11223344557",
    dialogId: "2",
    createdAt: new Date(2024, 0, 11).toString(),
  },
];
