import { Utkast } from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "./mock_tiltaksgjennomforinger";
import { mockAvtaler } from "./mock_avtaler";

export const mockUtkast: Utkast[] = [
  {
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
    createdAt: new Date("2023-05-17").toDateString(),
    updatedAt: new Date().toDateString(),
    id: "b9264c5c-27dc-43fd-81ff-c93eb1dceb12",
    opprettetAv: "B123456",
    type: Utkast.type.AVTALE,
    utkastData: { ...mockAvtaler[0] },
  },
  {
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("2023-07-06").toDateString(),
    updatedAt: new Date().toDateString(),
    id: "b9264c5c-27dc-43fd-81ff-c93eb1dceb14",
    opprettetAv: "B123456",
    type: Utkast.type.TILTAKSGJENNOMFORING,
    utkastData: {
      ...mockTiltaksgjennomforinger[1],
    },
  },
  {
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("2023-07-06").toDateString(),
    updatedAt: new Date().toDateString(),
    id: "b9264c5c-27dc-43fd-81ff-c93eb1dceb13",
    opprettetAv: "B123456",
    type: Utkast.type.AVTALE,
    utkastData: { ...mockAvtaler[1] },
  },
  {
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("2023-07-06").toDateString(),
    updatedAt: new Date().toDateString(),
    id: "b9264c5c-27dc-43fd-81ff-c93eb1dceb15",
    opprettetAv: "B123456",
    type: Utkast.type.TILTAKSGJENNOMFORING,
    utkastData: {
      ...mockTiltaksgjennomforinger[2],
    },
  },
  {
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("2023-07-06").toDateString(),
    updatedAt: new Date().toDateString(),
    id: "b9264c5c-27dc-43fd-81ff-c93eb1dceb16",
    opprettetAv: "B123456",
    type: Utkast.type.AVTALE,
    utkastData: { ...mockAvtaler[2] },
  },
  {
    avtaleId: "d1f163b7-1a41-4547-af16-03fd4492b7bc",
    createdAt: new Date("2023-07-06").toDateString(),
    updatedAt: new Date().toDateString(),
    id: "b9264c5c-27dc-43fd-81ff-c93eb1dceb17",
    opprettetAv: "B123456",
    type: Utkast.type.TILTAKSGJENNOMFORING,
    utkastData: {
      ...mockTiltaksgjennomforinger[3],
    },
  },
];
