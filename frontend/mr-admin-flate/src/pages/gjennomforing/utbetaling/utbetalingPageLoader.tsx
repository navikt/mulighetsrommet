import { UtbetalingService } from "@mr/api-client-v2";

export const utbetalingQuery = (utbetalingId?: string) => ({
  queryKey: ["utbetaling", utbetalingId],
  queryFn: () => UtbetalingService.getUtbetaling({ path: { id: utbetalingId! } }),
  enabled: !!utbetalingId,
});

export const tilsagnTilUtbetalingQuery = (utbetalingId?: string) => ({
  queryKey: ["utbetaling", utbetalingId, "tilsagn"],
  queryFn: () => UtbetalingService.getTilsagnTilUtbetaling({ path: { id: utbetalingId! } }),
  enabled: !!utbetalingId,
});

export const utbetalingHistorikkQuery = (utbetalingId?: string) => ({
  queryKey: ["utbetaling", utbetalingId, "historikk"],
  queryFn: () => UtbetalingService.getUtbetalingEndringshistorikk({ path: { id: utbetalingId! } }),
  enabled: !!utbetalingId,
});
