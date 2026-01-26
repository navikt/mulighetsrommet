import {
  Valuta,
  DataDrivenTableDto,
  DataElementTextFormat,
  LabeledDataElementType,
  TilsagnBeregningDto,
  TilsagnBeregningType,
  TilsagnDetaljerDto,
  TilsagnRequest,
  TilsagnType,
  TotrinnskontrollDtoTilBeslutning,
} from "@tiltaksadministrasjon/api-client";
import { http, HttpResponse, PathParams } from "msw";
import { mockTilsagn, mockTilsagnTable } from "../fixtures/mock_tilsagn";
import { v4 } from "uuid";
import { mockGjennomforinger } from "../fixtures/mock_gjennomforinger";

export const tilsagnHandlers = [
  http.get<PathParams, any, DataDrivenTableDto>("*/api/tiltaksadministrasjon/tilsagn", async () => {
    return HttpResponse.json(mockTilsagnTable);
  }),
  http.get<PathParams, any, TilsagnRequest>(
    "*/api/tiltaksadministrasjon/tilsagn/defaults",
    async () => {
      return HttpResponse.json({
        id: v4(),
        gjennomforingId: mockGjennomforinger[0].id,
        type: TilsagnType.TILSAGN,
        periodeStart: null,
        periodeSlutt: null,
        beregning: {
          type: TilsagnBeregningType.FRI,
          valuta: Valuta.NOK,
          antallPlasser: null,
          prisbetingelser: null,
          antallTimerOppfolgingPerDeltaker: null,
          linjer: [],
        },
        kostnadssted: null,
        kommentar: null,
        beskrivelse: null,
      });
    },
  ),
  http.get<PathParams, any, TilsagnDetaljerDto>(
    "*/api/tiltaksadministrasjon/tilsagn/:tilsagnId",
    async ({ params }) => {
      const { tilsagnId } = params;

      const tilsagn = mockTilsagn.find((t) => t.id === tilsagnId);
      if (!tilsagn) {
        return HttpResponse.json(undefined, { status: 404 });
      }

      return HttpResponse.json({
        tilsagn,
        beregning,
        opprettelse: tilBeslutning,
        annullering: null,
        tilOppgjor: null,
        handlinger: [],
      });
    },
  ),

  http.post<PathParams, any, string>(
    "*/api/tiltaksadministrasjon/tilsagn/:tilsagnId/beslutt",
    async () => {
      return HttpResponse.text("OK");
    },
  ),

  http.delete<PathParams, any, string>("*/api/tiltaksadministrasjon/tilsagn/:id", () => {
    return HttpResponse.text("Ok");
  }),
];

const tilBeslutning: TotrinnskontrollDtoTilBeslutning = {
  behandletAv: {
    navn: "Per Haraldsen",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  forklaring: null,
};

const beregning: TilsagnBeregningDto = {
  pris: {
    belop: 12207450,
    valuta: Valuta.NOK,
  },
  prismodell: {
    header: null,
    entries: [
      {
        type: LabeledDataElementType.INLINE,
        label: "Prismodell",
        value: {
          type: "DATA_ELEMENT_TEXT",
          value: "Fast sats per tiltaksplass per måned",
          format: null,
        },
      },
      {
        type: LabeledDataElementType.INLINE,
        label: "Antall plasser",
        value: {
          type: "DATA_ELEMENT_TEXT",
          value: "97",
          format: DataElementTextFormat.NUMBER,
        },
      },
      {
        type: LabeledDataElementType.INLINE,
        label: "Sats",
        value: {
          type: "DATA_ELEMENT_MONEY_AMOUNT",
          value: "20975",
          currency: "NOK",
        },
      },
    ],
  },
  regnestykke: {
    expression: [
      {
        type: "DATA_ELEMENT_TEXT",
        value: "97 plasser * 2099 per tiltaksplass per måned x 6.0 = 123000",
        format: DataElementTextFormat.NUMBER,
      },
    ],
    breakdown: null,
  },
};
