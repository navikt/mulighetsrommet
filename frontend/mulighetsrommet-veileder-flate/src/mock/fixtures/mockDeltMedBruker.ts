import { DelMedBruker } from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "./mockTiltaksgjennomforinger";

export const mockDeltMedBruker: DelMedBruker[] = [
  {
    sanityId: mockTiltaksgjennomforinger[0].sanityId,
    norskIdent: "11223344557",
    createdAt: new Date(2022, 2, 22).toString(),
  },
  {
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[2].id,
    norskIdent: "11223344557",
    createdAt: new Date(2024, 0, 11).toString(),
  },
];
