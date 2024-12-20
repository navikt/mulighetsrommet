import { GetAvtalteSatserResponse, TilsagnDto, TilsagnRequest } from "@mr/api-client";
import { http, HttpResponse, PathParams } from "msw";
import { mockTilsagn } from "../fixtures/mock_tilsagn";

export const tilsagnHandlers = [
  http.put<PathParams, TilsagnRequest>(
    "*/api/v1/intern/tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn",
    async ({ request }) => {
      const body = await request.json();
      return HttpResponse.json(body);
    },
  ),
  http.get<PathParams, any, TilsagnDto[]>(
    "*/api/v1/intern/tiltaksgjennomforinger/:tiltaksgjennomforingId/tilsagn",
    async () => {
      return HttpResponse.json(mockTilsagn);
    },
  ),
  http.get<PathParams, any, TilsagnDto>(
    "*/api/v1/intern/tilsagn/:tilsagnId",
    async ({ params }) => {
      const { tilsagnId } = params;

      const tilsagn = mockTilsagn.find((t) => t.id === tilsagnId);
      return HttpResponse.json(tilsagn);
    },
  ),

  http.post<PathParams, any, string>("*/api/v1/intern/tilsagn/:tilsagnId/beslutt", async () => {
    return HttpResponse.text("OK");
  }),

  http.get<PathParams, string, GetAvtalteSatserResponse>(
    "*/api/v1/intern/prismodell/satser",
    () => {
      return HttpResponse.json([
        {
          periodeStart: "2024-01-01",
          periodeSlutt: "2024-12-31",
          pris: 20205,
          valuta: "NOK",
        },
        {
          periodeStart: "2023-01-01",
          periodeSlutt: "2023-12-31",
          pris: 19500,
          valuta: "NOK",
        },
      ]);
    },
  ),

  http.delete<PathParams, any, string>("*/api/v1/intern/tilsagn/:id", () => {
    return HttpResponse.text("Ok");
  }),
];
