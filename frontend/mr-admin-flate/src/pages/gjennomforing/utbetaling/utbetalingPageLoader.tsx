import { UtbetalingService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";

export function useUtbetaling(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utbetaling(id),
    queryFn: async () => UtbetalingService.getUtbetaling({ path: { id } }),
  });
}

export function useUtbetalingsLinjer(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utbetalingsLinjer(id),
    queryFn: async () => UtbetalingService.getUtbetalingsLinjer({ path: { id } }),
  });
}

export function useUtbetalingEndringshistorikk(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utbetalingHistorikk(id),
    queryFn: async () => UtbetalingService.getUtbetalingEndringshistorikk({ path: { id } }),
  });
}

export function useUtbetalingBeregning(filter: { navEnheter: string[] }, id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.utbetalingBeregning(filter, id),
    queryFn: async () =>
      UtbetalingService.getUtbetalingBeregning({ path: { id }, query: { ...filter } }),
  });
}
