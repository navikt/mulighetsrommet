import { UtbetalingService as LegacyUtbetalingService } from "@mr/api-client-v2";
import { UtbetalingService } from "@tiltaksadministrasjon/api-client";
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
    queryFn: async () => LegacyUtbetalingService.getTilsagnTilUtbetaling({ path: { id } }),
  });
}

export function useUtbetalingEndringshistorikk(id: string) {
  return useApiSuspenseQuery({
    queryKey: ["utbetaling", id, "historikk"],
    queryFn: async () => LegacyUtbetalingService.getUtbetalingEndringshistorikk({ path: { id } }),
  });
}

export function useUtbetalingBeregning(filter: { navEnheter: string[] }, id: string) {
  return useApiSuspenseQuery({
    queryKey: ["utbetaling-beregning", id, filter, filter.navEnheter.join(",")],
    queryFn: async () =>
      LegacyUtbetalingService.getUtbetalingBeregning({ path: { id }, query: { ...filter } }),
  });
}
