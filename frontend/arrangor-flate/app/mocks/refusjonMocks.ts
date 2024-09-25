import { RefusjonskravDto } from "@mr/api-client";
import { http, HttpResponse, PathParams } from "msw";
import { v4 as uuid } from "uuid";

const mockKrav: RefusjonskravDto[] = [
  {
    id: uuid(),
    beregning: {
      type: "AFT",
      sats: 20205,
      belop: 308530,
    },
    periodeStart: "01.06.2024",
    periodeSlutt: "30.06.2024",
    tiltaksgjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Moss",
    },
  },
  {
    id: uuid(),
    beregning: {
      type: "AFT",
      sats: 20205,
      belop: 18530,
    },
    periodeStart: "01.05.2024",
    periodeSlutt: "31.05.2024",
    tiltaksgjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Moss",
    },
  },
  {
    id: uuid(),
    beregning: {
      type: "AFT",
      sats: 20205,
      belop: 85000,
    },
    periodeStart: "01.01.2024",
    periodeSlutt: "31.01.2024",
    tiltaksgjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
    },
  },
];

export const refusjonHandlers = [
  http.get<PathParams, RefusjonskravDto[]>("*/api/v1/intern/refusjon/:orgnr/krav", () =>
    HttpResponse.json(mockKrav),
  ),
  http.get<PathParams, RefusjonskravDto[]>("*/api/v1/intern/refusjon/krav/:id", () =>
    HttpResponse.json(mockKrav[1]),
  ),
];
