import { UtbetalingDetaljerDto, TilsagnDto, UtbetalingKompaktDto } from "@mr/api-client-v2";
import { http, HttpResponse, PathParams } from "msw";
import {
  mockUtbetalinger,
  mockUtbetalingerKompakt,
  mockUtbetalingLinjer,
} from "../fixtures/mock_utbetalinger";
import { mockTilsagn } from "../fixtures/mock_tilsagn";

export const utbetalingHandlers = [
  http.get<PathParams, PathParams, UtbetalingDetaljerDto>(
    "*/api/v1/intern/utbetaling/:id",
    ({ params }) => {
      const { id } = params;
      const mockUtbetaling = mockUtbetalinger.find((u) => u.id === id);
      if (!mockUtbetaling) throw Error(`Fant ikke mocket utbetaling med id: ${id}`);

      // Find matching linjer for this utbetaling based on periode
      const matchingLinjer = mockUtbetalingLinjer.filter(
        (linje) =>
          linje.tilsagn.periode.start === mockUtbetaling.periode.start &&
          linje.tilsagn.periode.slutt === mockUtbetaling.periode.slutt,
      );

      return HttpResponse.json({
        utbetaling: mockUtbetaling,
        deltakere: [],
        linjer: matchingLinjer,
      });
    },
  ),
  http.get<PathParams, PathParams, UtbetalingKompaktDto[]>(
    "*/api/v1/intern/gjennomforinger/:id/utbetalinger",
    () => {
      return HttpResponse.json(mockUtbetalingerKompakt);
    },
  ),
  http.get<PathParams, TilsagnDto[], TilsagnDto[]>("*/api/v1/intern/utbetaling/:id/tilsagn", () => {
    return HttpResponse.json(mockTilsagn);
  }),
  http.post<PathParams>("*/api/v1/intern/utbetaling/:id/opprett-utbetaling", () => {
    return HttpResponse.json("ok");
  }),
];
