import { useSuspenseQuery } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient } from "~/api/client";
import { queryKeys } from "~/api/queryKeys";

export function useOpprettKravData(orgnr: string, gjennomforingId: string) {
  return useSuspenseQuery({
    queryKey: queryKeys.opprettKravData(orgnr, gjennomforingId),
    queryFn: async () => {
      const result = await ArrangorflateService.getOpprettKravData({
        path: { orgnr, gjennomforingId },
        client: queryClient,
      });
      if (result.error) {
        throw result.error;
      }
      return result.data;
    },
  });
}
