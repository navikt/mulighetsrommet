import {
  GetForhandsgodkjenteSatserResponse,
  TilsagnAvvisningAarsak,
  TilsagnDefaults,
  TilsagnDetaljerDto,
  TilsagnDto,
  TilsagnRequest,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
} from "@mr/api-client-v2";
import { http, HttpResponse, PathParams } from "msw";
import { mockTilsagn } from "../fixtures/mock_tilsagn";

export const tilsagnHandlers = [
  http.put<PathParams, TilsagnRequest>(
    "*/api/v1/intern/gjennomforinger/:gjennomforingId/tilsagn",
    async ({ request }) => {
      const body = await request.json();
      return HttpResponse.json(body);
    },
  ),
  http.get<PathParams, any, TilsagnDto[]>(
    "*/api/v1/intern/gjennomforinger/:gjennomforingId/tilsagn",
    async () => {
      return HttpResponse.json(mockTilsagn);
    },
  ),
  http.get<PathParams, any, TilsagnDto[]>("*/api/v1/intern/tilsagn", async () => {
    return HttpResponse.json(mockTilsagn);
  }),
  http.get<PathParams, any, TilsagnDefaults>("*/api/v1/intern/tilsagn/defaults", async () => {
    return HttpResponse.json({});
  }),
  http.get<PathParams, any, TilsagnDetaljerDto>(
    "*/api/v1/intern/tilsagn/:tilsagnId",
    async ({ params }) => {
      const { tilsagnId } = params;

      const tilsagn = mockTilsagn.find((t) => t.id === tilsagnId);
      if (!tilsagn) {
        return HttpResponse.json(undefined, { status: 404 });
      }

      return HttpResponse.json(toTilsagnDetaljerDto(tilsagn));
    },
  ),

  http.post<PathParams, any, string>("*/api/v1/intern/tilsagn/:tilsagnId/beslutt", async () => {
    return HttpResponse.text("OK");
  }),

  http.get<PathParams, string, GetForhandsgodkjenteSatserResponse>(
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

function toTilsagnDetaljerDto(tilsagn: TilsagnDto): TilsagnDetaljerDto {
  switch (tilsagn.status) {
    case TilsagnStatus.TIL_GODKJENNING:
      return {
        tilsagn,
        opprettelse: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
        },
      };

    case TilsagnStatus.GODKJENT:
      return {
        tilsagn,
        opprettelse: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
          besluttetAv: "F123456",
          besluttetTidspunkt: "2024-01-01T22:00:00",
        },
      };

    case TilsagnStatus.RETURNERT:
      return {
        tilsagn,
        opprettelse: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-09",
          besluttetAv: "N12345",
          besluttetTidspunkt: "2024-01-10",
          aarsaker: [TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER, TilsagnAvvisningAarsak.FEIL_ANNET],
          forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
        },
      };

    case TilsagnStatus.TIL_ANNULLERING:
      return {
        tilsagn,
        opprettelse: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
          besluttetAv: "F123456",
          besluttetTidspunkt: "2024-01-01T22:00:00",
        },
        annullering: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
          aarsaker: [
            TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
            TilsagnTilAnnulleringAarsak.FEIL_ANNET,
          ],
          forklaring: "Du må fikse det",
        },
      };

    case TilsagnStatus.ANNULLERT:
      return {
        tilsagn,
        opprettelse: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
          besluttetAv: "F123456",
          besluttetTidspunkt: "2024-01-01T22:00:00",
        },
        annullering: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
          aarsaker: [
            TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
            TilsagnTilAnnulleringAarsak.FEIL_ANNET,
          ],
          forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
          besluttetAv: "F123456",
          besluttetTidspunkt: "2024-01-01T22:00:00",
        },
      };

    case TilsagnStatus.TIL_OPPGJOR:
      return {
        tilsagn,
        opprettelse: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
          besluttetAv: "F123456",
          besluttetTidspunkt: "2024-01-01T22:00:00",
        },
        tilOppgjor: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
        },
      };

    case TilsagnStatus.OPPGJORT:
      return {
        tilsagn,
        opprettelse: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
          besluttetAv: "F123456",
          besluttetTidspunkt: "2024-01-01T22:00:00",
        },
        tilOppgjor: {
          behandletAv: "B123456",
          behandletTidspunkt: "2024-01-01T22:00:00",
          besluttetAv: "F123456",
          besluttetTidspunkt: "2024-01-01T22:00:00",
        },
      };
  }
}
