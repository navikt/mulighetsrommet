import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrangorflateService, TiltaksoversiktType } from "api-client";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

export function useArrangorTiltaksoversikt(type: TiltaksoversiktType) {
  return useSuspenseQuery({
    queryKey: queryKeys.tiltaksoversikt(type),
    queryFn: async () => {
      const result = await ArrangorflateService.getArrangorTiltaksoversikt({
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
