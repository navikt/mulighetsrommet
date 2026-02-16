import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

export function useUtbetalingKvittering(id: string) {
  return useSuspenseQuery({
    queryKey: queryKeys.utbetalingKvittering(id),
    queryFn: async () => {
      const result = await ArrangorflateService.getUtbetalingKvittering({
        path: { id },
        client: queryClient,
      });
      if (result.error) {
        throw result.error;
      }
      return result.data;
    },
    retry: false,
  });
}
