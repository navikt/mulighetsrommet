import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient as apiClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

export function useOrganisasjonsTilganger() {
  return useSuspenseQuery({
    queryKey: queryKeys.orgnrTilganger(),
    queryFn: async () => {
      const result = await ArrangorflateService.getOrganisasjonTilganger({
        client: apiClient,
      });
      if (result.error) {
        throw result.error;
      }
      return result.data;
    },
    staleTime: 10 * 1000, // 10s
  });
}
