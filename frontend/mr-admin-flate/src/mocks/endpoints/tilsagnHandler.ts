import {
  Besluttelse,
  DataDrivenTableDto,
  DataElementMathOperatorType,
  DataElementTextFormat,
  GetForhandsgodkjenteSatserResponse,
  LabeledDataElementType,
  TilsagnAvvisningAarsak,
  TilsagnBeregningDto,
  TilsagnBeregningType,
  TilsagnDetaljerDto,
  TilsagnDto,
  TilsagnRequest,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TilsagnType,
  TotrinnskontrollBesluttetDto,
  TotrinnskontrollTilBeslutningDto,
} from "@mr/api-client-v2";
import { http, HttpResponse, PathParams } from "msw";
import { mockTilsagn, mockTilsagnTable } from "../fixtures/mock_tilsagn";
import { v4 } from "uuid";
import { mockGjennomforinger } from "../fixtures/mock_gjennomforinger";

export const tilsagnHandlers = [
  http.put<PathParams, TilsagnRequest>(
    "*/api/v1/intern/gjennomforinger/:gjennomforingId/tilsagn",
    async ({ request }) => {
      const body = await request.json();
      return HttpResponse.json(body);
    },
  ),
  http.get<PathParams, any, DataDrivenTableDto>("*/api/v1/intern/tilsagn", async () => {
    return HttpResponse.json(mockTilsagnTable);
  }),
  http.get<PathParams, any, TilsagnRequest>("*/api/v1/intern/tilsagn/defaults", async () => {
    return HttpResponse.json({
      type: TilsagnType.TILSAGN,
      gjennomforingId: mockGjennomforinger[0].id,
      id: v4(),
      beregning: { type: TilsagnBeregningType.FRI },
    });
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
  behandletAv: {
    type: "NAV_ANSATT",
    navn: "Per Haraldsen",
    navIdent: "P654321",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  kanBesluttes: true,
};

const godkjent: TotrinnskontrollBesluttetDto = {
  type: "BESLUTTET",
  besluttetAv: {
    type: "NAV_ANSATT",
    navn: "Per Haraldsen",
    navIdent: "P654321",
  },
  behandletAv: {
    type: "NAV_ANSATT",
    navn: "Bertil Bengtson",
    navIdent: "B123456",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  besluttetTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  kanBesluttes: false,
  besluttelse: Besluttelse.GODKJENT,
};

const avvist: TotrinnskontrollBesluttetDto = {
  type: "BESLUTTET",
  besluttetAv: {
    type: "NAV_ANSATT",
    navn: "Per Haraldsen",
    navIdent: "P654321",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  behandletAv: {
    type: "NAV_ANSATT",
    navn: "Bertil Bengtson",
    navIdent: "B123456",
  },
  besluttetTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  kanBesluttes: false,
  besluttelse: Besluttelse.AVVIST,
};

const beregning: TilsagnBeregningDto = {
  belop: 12207450,
  prismodell: {
    entries: [
      {
        type: LabeledDataElementType.INLINE,
        label: "Prismodell",
        value: {
          type: "text",
          value: "Fast sats per tiltaksplass per måned",
          format: null,
        },
      },
      {
        type: LabeledDataElementType.INLINE,
        label: "Antall plasser",
        value: { type: "text", value: "97", format: DataElementTextFormat.NUMBER },
      },
      {
        type: LabeledDataElementType.INLINE,
        label: "Sats",
        value: { type: "text", value: "20975", format: DataElementTextFormat.NOK },
      },
    ],
  },
  regnestykke: {
    expression: [
      { type: "text", value: "97", format: DataElementTextFormat.NUMBER },
      { type: "text", value: "plasser", format: null },
      { type: "math-operator", operator: DataElementMathOperatorType.MULTIPLY },
      { type: "text", value: "20975", format: DataElementTextFormat.NOK },
      { type: "text", value: "per tiltaksplass per måned", format: null },
      { type: "math-operator", operator: DataElementMathOperatorType.MULTIPLY },
      { type: "text", value: "6.0", format: DataElementTextFormat.NUMBER },
      { type: "text", value: "måneder", format: null },
      { type: "math-operator", operator: DataElementMathOperatorType.EQUALS },
      { type: "text", value: "12207450", format: DataElementTextFormat.NOK },
    ],
  },
};

function toTilsagnDetaljerDto(tilsagn: TilsagnDto): TilsagnDetaljerDto {
  switch (tilsagn.status) {
    case TilsagnStatus.TIL_GODKJENNING:
      return {
        tilsagn,
        beregning,
        opprettelse: tilBeslutning,
      };

    case TilsagnStatus.GODKJENT:
      return {
        tilsagn,
        beregning,
        opprettelse: godkjent,
      };

    case TilsagnStatus.RETURNERT:
      return {
        tilsagn,
        beregning,
        opprettelse: {
          ...avvist,
          aarsaker: [TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER, TilsagnAvvisningAarsak.ANNET],
          forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
        },
      };

    case TilsagnStatus.TIL_ANNULLERING:
      return {
        tilsagn,
        beregning,
        opprettelse: godkjent,
        annullering: {
          ...tilBeslutning,
          aarsaker: [
            TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
            TilsagnTilAnnulleringAarsak.ANNET,
          ],
          forklaring: "Du må fikse det",
        },
      };

    case TilsagnStatus.ANNULLERT:
      return {
        tilsagn,
        beregning,
        opprettelse: godkjent,
        annullering: {
          ...godkjent,
          aarsaker: [
            TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
            TilsagnTilAnnulleringAarsak.ANNET,
          ],
          forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
        },
      };

    case TilsagnStatus.TIL_OPPGJOR:
      return {
        tilsagn,
        beregning,
        opprettelse: godkjent,
        tilOppgjor: tilBeslutning,
      };

    case TilsagnStatus.OPPGJORT:
      return {
        beregning,
        tilsagn,
        opprettelse: godkjent,
        tilOppgjor: godkjent,
      };
  }
}
