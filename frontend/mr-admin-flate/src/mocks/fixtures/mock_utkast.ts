import { Utkast } from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "./mock_tiltaksgjennomforinger";

export const mockUtkast: Utkast[] = [
  {
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
    createdAt: new Date("2023-05-17").toDateString(),
    updatedAt: new Date().toDateString(),
    id: "b9264c5c-27dc-43fd-81ff-c93eb1dceb12",
    opprettetAv: "B123456",
    type: Utkast.type.TILTAKSGJENNOMFORING,
    utkastData: { ...mockTiltaksgjennomforinger.data[0] },
  },
  {
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("2023-07-06").toDateString(),
    updatedAt: new Date().toDateString(),
    id: "b9264c5c-27dc-43fd-81ff-c93eb1dceb14",
    opprettetAv: "B123456",
    type: Utkast.type.TILTAKSGJENNOMFORING,
    utkastData: { ...mockTiltaksgjennomforinger.data[1] },
  },
];
