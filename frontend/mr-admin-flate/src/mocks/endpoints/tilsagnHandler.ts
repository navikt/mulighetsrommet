import {
  DataDrivenTableDto,
  DataElementMathOperatorType,
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
          antallPlasser: null,
          prisbetingelser: null,
          antallTimerOppfolgingPerDeltaker: null,
          linjer: [],
        },
        kostnadssted: null,
        kommentar: null,
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
  belop: 12207450,
  prismodell: {
    header: null,
    entries: [
      {
        type: LabeledDataElementType.INLINE,
        label: "Prismodell",
        value: {
          type: "no.nav.mulighetsrommet.model.DataElement.Text",
          value: "Fast sats per tiltaksplass per måned",
          format: null,
        },
      },
      {
        type: LabeledDataElementType.INLINE,
        label: "Antall plasser",
        value: {
          type: "no.nav.mulighetsrommet.model.DataElement.Text",
          value: "97",
          format: DataElementTextFormat.NUMBER,
        },
      },
      {
        type: LabeledDataElementType.INLINE,
        label: "Sats",
        value: {
          type: "no.nav.mulighetsrommet.model.DataElement.Text",
          value: "20975",
          format: DataElementTextFormat.NOK,
        },
      },
    ],
  },
  regnestykke: {
    expression: [
      {
        type: "no.nav.mulighetsrommet.model.DataElement.Text",
        value: "97",
        format: DataElementTextFormat.NUMBER,
      },
      { type: "no.nav.mulighetsrommet.model.DataElement.Text", value: "plasser", format: null },
      {
        type: "no.nav.mulighetsrommet.model.DataElement.MathOperator",
        operator: DataElementMathOperatorType.MULTIPLY,
      },
      {
        type: "no.nav.mulighetsrommet.model.DataElement.Text",
        value: "20975",
        format: DataElementTextFormat.NOK,
      },
      {
        type: "no.nav.mulighetsrommet.model.DataElement.Text",
        value: "per tiltaksplass per måned",
        format: null,
      },
      {
        type: "no.nav.mulighetsrommet.model.DataElement.MathOperator",
        operator: DataElementMathOperatorType.MULTIPLY,
      },
      {
        type: "no.nav.mulighetsrommet.model.DataElement.Text",
        value: "6.0",
        format: DataElementTextFormat.NUMBER,
      },
      { type: "no.nav.mulighetsrommet.model.DataElement.Text", value: "måneder", format: null },
      {
        type: "no.nav.mulighetsrommet.model.DataElement.MathOperator",
        operator: DataElementMathOperatorType.EQUALS,
      },
      {
        type: "no.nav.mulighetsrommet.model.DataElement.Text",
        value: "12207450",
        format: DataElementTextFormat.NOK,
      },
    ],
    breakdown: null,
  },
};
