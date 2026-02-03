import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient } from "~/api/client";

export const utbetalingQueryKeys = {
  all: ["utbetaling"] as const,
  detail: (id: string) => [...utbetalingQueryKeys.all, id] as const,
  tilsagn: (id: string) => [...utbetalingQueryKeys.all, id, "tilsagn"] as const,
};

export function useArrangorflateUtbetaling(id: string) {
  return useSuspenseQuery({
    queryKey: utbetalingQueryKeys.detail(id),
    queryFn: async () => {
      const result = await ArrangorflateService.getArrangorflateUtbetaling({
        path: { id },
        client: queryClient,
      });
      if (result.error) {
        throw result.error;
      }
      return result.data;
    },
  });
}

export function useArrangorflateTilsagnTilUtbetaling(id: string) {
  return useSuspenseQuery({
    queryKey: utbetalingQueryKeys.tilsagn(id),
    queryFn: async () => {
      const result = await ArrangorflateService.getArrangorflateTilsagnTilUtbetaling({
        path: { id },
        client: queryClient,
      });
      if (result.error) {
        throw result.error;
      }
      return result.data;
    },
  });
}
