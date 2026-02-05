import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

export function useArrangorflateTilsagnTilUtbetaling(id: string) {
  return useSuspenseQuery({
    queryKey: queryKeys.utbetalingTilsagn(id),
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
