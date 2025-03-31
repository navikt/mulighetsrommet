import { UtbetalingDetaljerDto, UtbetalingDto, TilsagnDto } from "@mr/api-client-v2";
import { http, HttpResponse, PathParams } from "msw";
import { mockUtbetalinger } from "../fixtures/mock_utbetalinger";
import { mockTilsagn } from "../fixtures/mock_tilsagn";

export const utbetalingHandlers = [
  http.get<PathParams, PathParams, UtbetalingDetaljerDto>("*/api/v1/intern/utbetaling/:id", () => {
    return HttpResponse.json({
      utbetaling: mockUtbetalinger[0],
      deltakere: [],
      linjer: [],
    });
  }),
  http.get<PathParams, PathParams, UtbetalingDto[]>(
    "*/api/v1/intern/gjennomforinger/:id/utbetalinger",
    () => {
      return HttpResponse.json(mockUtbetalinger);
    },
  ),
  http.get<PathParams, TilsagnDto[], TilsagnDto[]>("*/api/v1/intern/utbetaling/:id/tilsagn", () => {
    return HttpResponse.json(mockTilsagn);
  }),
  http.post<PathParams>("*/api/v1/intern/utbetaling/:id/opprett-utbetaling", () => {
    return HttpResponse.json("ok");
  }),
];
