import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrangorflateService, UtbetalingOversiktType } from "api-client";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

export function useArrangorflateUtbetalinger(type: UtbetalingOversiktType) {
  return useSuspenseQuery({
    queryKey: queryKeys.utbetalinger(type),
    queryFn: async () => {
      const result = await ArrangorflateService.getArrangorflateUtbetalinger({
        query: { type },
        client: queryClient,
      });
      if (result.error) {
        throw result.error;
      }
      return result.data;
    },
  });
}
