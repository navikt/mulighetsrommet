import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

export function useArrangorflateUtbetaling(id: string) {
  return useSuspenseQuery({
    queryKey: queryKeys.utbetaling(id),
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
