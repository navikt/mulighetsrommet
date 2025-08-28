import { UtbetalingService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useUtbetaling(id: string) {
  return useApiSuspenseQuery({
    queryKey: ["utbetaling", id],
    queryFn: async () => UtbetalingService.getUtbetaling({ path: { id } }),
  });
}

export function useTilsagnTilUtbetaling(id: string) {
  return useApiSuspenseQuery({
    queryKey: ["utbetaling", id, "tilsagn"],
    queryFn: async () => UtbetalingService.getTilsagnTilUtbetaling({ path: { id } }),
  });
}

export function useUtbetalingEndringshistorikk(id: string) {
  return useApiSuspenseQuery({
    queryKey: ["utbetaling", id, "historikk"],
    queryFn: async () => UtbetalingService.getUtbetalingEndringshistorikk({ path: { id } }),
  });
}

export function useUtbetalingBeregning(filter: { navEnheter: string[] }, id: string) {
  return useApiSuspenseQuery({
    queryKey: ["utbetaling-beregning", id, filter, filter.navEnheter.join(",")],
    queryFn: async () =>
      UtbetalingService.getUtbetalingBeregning({ path: { id }, query: { ...filter } }),
  });
}
