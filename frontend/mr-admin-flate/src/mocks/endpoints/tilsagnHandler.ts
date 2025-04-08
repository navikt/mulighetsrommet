import {
  Besluttelse,
  GetForhandsgodkjenteSatserResponse,
  TilsagnAvvisningAarsak,
  TilsagnDefaults,
  TilsagnDetaljerDto,
  TilsagnDto,
  TilsagnRequest,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TotrinnskontrollBesluttetDto,
  TotrinnskontrollTilBeslutningDto,
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

const tilBeslutning: TotrinnskontrollTilBeslutningDto = {
  type: "TIL_BESLUTNING",
  behandletAvMetadata: {
    type: "P654321",
    navn: "Per Haraldsen",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  kanBesluttes: true,
};

const godkjent: TotrinnskontrollBesluttetDto = {
  type: "BESLUTTET",
  besluttetAvMetadata: {
    type: "P654321",
    navn: "Per Haraldsen",
  },
  behandletAvMetadata: {
    type: "B123456",
    navn: "Bertil Bengtson",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  besluttetTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  kanBesluttes: false,
  besluttelse: Besluttelse.GODKJENT,
};

const avvist: TotrinnskontrollBesluttetDto = {
  type: "BESLUTTET",
  besluttetAvMetadata: {
    type: "P654321",
    navn: "Per Haraldsen",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  behandletAvMetadata: {
    type: "B123456",
    navn: "Bertil Bengtson",
  },
  besluttetTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  kanBesluttes: false,
  besluttelse: Besluttelse.AVVIST,
};

function toTilsagnDetaljerDto(tilsagn: TilsagnDto): TilsagnDetaljerDto {
  switch (tilsagn.status) {
    case TilsagnStatus.TIL_GODKJENNING:
      return {
        tilsagn,
        opprettelse: tilBeslutning,
      };

    case TilsagnStatus.GODKJENT:
      return {
        tilsagn,
        opprettelse: godkjent,
      };

    case TilsagnStatus.RETURNERT:
      return {
        tilsagn,
        opprettelse: {
          ...avvist,
          aarsaker: [TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER, TilsagnAvvisningAarsak.FEIL_ANNET],
          forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
        },
      };

    case TilsagnStatus.TIL_ANNULLERING:
      return {
        tilsagn,
        opprettelse: godkjent,
        annullering: {
          ...tilBeslutning,
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
        opprettelse: godkjent,
        annullering: {
          ...godkjent,
          aarsaker: [
            TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
            TilsagnTilAnnulleringAarsak.FEIL_ANNET,
          ],
          forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
        },
      };

    case TilsagnStatus.TIL_OPPGJOR:
      return {
        tilsagn,
        opprettelse: godkjent,
        tilOppgjor: tilBeslutning,
      };

    case TilsagnStatus.OPPGJORT:
      return {
        tilsagn,
        opprettelse: godkjent,
        tilOppgjor: godkjent,
      };
  }
}
