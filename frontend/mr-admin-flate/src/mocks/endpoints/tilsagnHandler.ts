import { AftSatserResponse, TilsagnDto, TilsagnRequest } from "@mr/api-client";
import { HttpResponse, PathParams, http } from "msw";
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

  http.get<PathParams, string, AftSatserResponse>("*/api/v1/intern/tilsagn/aft/sats", () => {
    return HttpResponse.json([
      {
        belop: 20205,
        startDato: "01-01-2024",
      },
      {
        belop: 19500,
        startDato: "01-01-2023",
      },
    ]);
  }),

  http.delete<PathParams, any, string>("*/api/v1/intern/tilsagn/:id", () => {
    return HttpResponse.text("Ok");
  }),
];
