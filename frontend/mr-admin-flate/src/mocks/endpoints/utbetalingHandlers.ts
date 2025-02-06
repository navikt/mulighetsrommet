import { TilsagnDto, Utbetaling } from "@mr/api-client-v2";
import { http, HttpResponse, PathParams } from "msw";
import { mockUtbetalinger } from "../fixtures/mock_utbetalinger";
import { mockTilsagn } from "../fixtures/mock_tilsagn";

export const utbetalingHandlers = [
  http.get<PathParams, PathParams, Utbetaling>("*/api/v1/intern/utbetaling/:id", () => {
    return HttpResponse.json(mockUtbetalinger[0]);
  }),
  http.get<PathParams, PathParams, Utbetaling[]>(
    "*/api/v1/intern/gjennomforinger/:id/utbetalinger",
    () => {
      return HttpResponse.json(mockUtbetalinger);
    },
  ),
  http.get<PathParams, TilsagnDto[], TilsagnDto[]>("*/api/v1/intern/utbetaling/:id/tilsagn", () => {
    return HttpResponse.json(mockTilsagn);
  }),
];
