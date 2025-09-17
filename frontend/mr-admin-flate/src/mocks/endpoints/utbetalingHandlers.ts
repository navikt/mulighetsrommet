import {
  TilsagnDto,
  UtbetalingBeregningDto,
  UtbetalingDetaljerDto,
  UtbetalingKompaktDto,
  UtbetalingHandling,
  UtbetalingLinje,
} from "@tiltaksadministrasjon/api-client";
import { http, HttpResponse, PathParams } from "msw";
import {
  mockBeregning,
  mockUtbetalinger,
  mockUtbetalingerKompakt,
  mockUtbetalingLinjer,
} from "../fixtures/mock_utbetalinger";
import { mockTilsagn } from "../fixtures/mock_tilsagn";

export const utbetalingHandlers = [
  http.get<PathParams, PathParams, UtbetalingKompaktDto[]>(
    "*/api/tiltaksadministrasjon/utbetaling",
    () => {
      return HttpResponse.json(mockUtbetalingerKompakt);
    },
  ),
  http.get<PathParams, PathParams, UtbetalingDetaljerDto>(
    "*/api/tiltaksadministrasjon/utbetaling/:id",
    ({ params }) => {
      const { id } = params;
      const mockUtbetaling = mockUtbetalinger.find((u) => u.id === id);
      if (!mockUtbetaling) throw Error(`Fant ikke mocket utbetaling med id: ${id}`);

      return HttpResponse.json({
        utbetaling: mockUtbetaling,
        handlinger:
          mockUtbetaling.status.type === "KLAR_TIL_BEHANDLING"
            ? [UtbetalingHandling.SEND_TIL_ATTESTERING]
            : [],
      });
    },
  ),
  http.get<PathParams, PathParams, UtbetalingLinje[]>(
    "*/api/tiltaksadministrasjon/utbetaling/:id/linjer",
    ({ params }) => {
      const { id } = params;
      const mockUtbetaling = mockUtbetalinger.find((u) => u.id === id);
      if (!mockUtbetaling) throw Error(`Fant ikke mocket utbetaling med id: ${id}`);

      const matchingLinjer = mockUtbetalingLinjer.filter(
        (linje) =>
          linje.tilsagn.periode.start === mockUtbetaling.periode.start &&
          linje.tilsagn.periode.slutt === mockUtbetaling.periode.slutt,
      );
      return HttpResponse.json(matchingLinjer);
    },
  ),
  http.get<PathParams, PathParams, UtbetalingBeregningDto>(
    "*/api/tiltaksadministrasjon/utbetaling/:id/beregning",
    () => {
      return HttpResponse.json(mockBeregning);
    },
  ),
  http.get<PathParams, TilsagnDto[], TilsagnDto[]>(
    "*/api/tiltaksadministrasjon/utbetaling/:id/tilsagn",
    () => {
      return HttpResponse.json(mockTilsagn);
    },
  ),
  http.post<PathParams>("*/api/tiltaksadministrasjon/utbetaling/:id/opprett-utbetaling", () => {
    return HttpResponse.json("ok");
  }),
];
