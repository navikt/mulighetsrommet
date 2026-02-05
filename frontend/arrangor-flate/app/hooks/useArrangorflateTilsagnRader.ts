import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

export function useArrangorflateTilsagnRader() {
  return useSuspenseQuery({
    queryKey: queryKeys.tilsagnRader(),
    queryFn: async () => {
      const result = await ArrangorflateService.getArrangorflateTilsagnRader({
        client: queryClient,
      });
      if (result.error) {
        throw result.error;
      }
      return result.data;
    },
  });
}
