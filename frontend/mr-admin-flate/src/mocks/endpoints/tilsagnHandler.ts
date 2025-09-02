import { GetForhandsgodkjenteSatserResponse } from "@mr/api-client-v2";
import { http, HttpResponse, PathParams } from "msw";
import { mockTilsagn, mockTilsagnTable } from "../fixtures/mock_tilsagn";
import { v4 } from "uuid";
import { mockGjennomforinger } from "../fixtures/mock_gjennomforinger";
import {
  TotrinnskontrollDtoBesluttet,
  TotrinnskontrollDtoTilBeslutning,
  TilsagnStatusAarsak,
  TilsagnBeregningDto,
  TilsagnBeregningType,
  TilsagnDetaljerDto,
  TilsagnDto,
  TilsagnRequest,
  TilsagnStatus,
  TilsagnType,
  DataDrivenTableDto,
  LabeledDataElementType,
  DataElementTextFormat,
  DataElementMathOperatorType,
  Besluttelse,
} from "@tiltaksadministrasjon/api-client";

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

      return HttpResponse.json(toTilsagnDetaljerDto(tilsagn));
    },
  ),

  http.post<PathParams, any, string>(
    "*/api/tiltaksadministrasjon/tilsagn/:tilsagnId/beslutt",
    async () => {
      return HttpResponse.text("OK");
    },
  ),

  http.get<PathParams, string, GetForhandsgodkjenteSatserResponse>(
    "*/api/tiltaksadministrasjon/prismodell/satser",
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

  http.delete<PathParams, any, string>("*/api/tiltaksadministrasjon/tilsagn/:id", () => {
    return HttpResponse.text("Ok");
  }),
];

const tilBeslutning: TotrinnskontrollDtoTilBeslutning = {
  behandletAv: {
    type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.AgentDto.NavAnsatt",
    navn: "Per Haraldsen",
    navIdent: "P654321",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  kanBesluttes: true,
  forklaring: null,
};

const godkjent: TotrinnskontrollDtoBesluttet = {
  besluttetAv: {
    type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.AgentDto.NavAnsatt",
    navn: "Per Haraldsen",
    navIdent: "P654321",
  },
  behandletAv: {
    type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.AgentDto.NavAnsatt",
    navn: "Bertil Bengtson",
    navIdent: "B123456",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  besluttetTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  besluttelse: Besluttelse.GODKJENT,
  forklaring: null,
};

const avvist: TotrinnskontrollDtoBesluttet = {
  besluttetAv: {
    type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.AgentDto.NavAnsatt",
    navn: "Per Haraldsen",
    navIdent: "P654321",
  },
  behandletTidspunkt: "2024-01-01T22:00:00",
  behandletAv: {
    type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.AgentDto.NavAnsatt",
    navn: "Bertil Bengtson",
    navIdent: "B123456",
  },
  besluttetTidspunkt: "2024-01-01T22:00:00",
  aarsaker: [],
  besluttelse: Besluttelse.AVVIST,
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

function toTilsagnDetaljerDto(tilsagn: TilsagnDto): TilsagnDetaljerDto {
  switch (tilsagn.status) {
    case TilsagnStatus.TIL_GODKJENNING:
      return {
        tilsagn,
        beregning,
        opprettelse: tilBeslutning,
        annullering: null,
        tilOppgjor: null,
      };

    case TilsagnStatus.GODKJENT:
      return {
        tilsagn,
        beregning,
        opprettelse: godkjent,
        annullering: null,
        tilOppgjor: null,
      };

    case TilsagnStatus.RETURNERT:
      return {
        tilsagn,
        beregning,
        opprettelse: {
          ...avvist,
          aarsaker: [TilsagnStatusAarsak.FEIL_ANTALL_PLASSER, TilsagnStatusAarsak.ANNET],
          forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
        },
        annullering: null,
        tilOppgjor: null,
      };

    case TilsagnStatus.TIL_ANNULLERING:
      return {
        tilsagn,
        beregning,
        opprettelse: godkjent,
        annullering: {
          ...tilBeslutning,
          aarsaker: [TilsagnStatusAarsak.FEIL_REGISTRERING, TilsagnStatusAarsak.ANNET],
          forklaring: "Du må fikse det",
        },
        tilOppgjor: null,
      };

    case TilsagnStatus.ANNULLERT:
      return {
        tilsagn,
        beregning,
        opprettelse: godkjent,
        annullering: {
          ...godkjent,
          aarsaker: [TilsagnStatusAarsak.FEIL_REGISTRERING, TilsagnStatusAarsak.ANNET],
          forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
        },
        tilOppgjor: null,
      };

    case TilsagnStatus.TIL_OPPGJOR:
      return {
        tilsagn,
        beregning,
        opprettelse: godkjent,
        annullering: null,
        tilOppgjor: tilBeslutning,
      };

    case TilsagnStatus.OPPGJORT:
      return {
        beregning,
        tilsagn,
        opprettelse: godkjent,
        annullering: null,
        tilOppgjor: godkjent,
      };
  }
}
