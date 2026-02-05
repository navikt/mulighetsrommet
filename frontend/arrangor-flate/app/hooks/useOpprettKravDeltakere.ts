import { useMutation } from "@tanstack/react-query";
import { ArrangorflateService } from "api-client";
import { queryClient } from "~/api/client";

interface FetchDeltakereParams {
  orgnr: string;
  gjennomforingId: string;
  periodeStart: string;
  periodeSlutt: string;
}

export function useOpprettKravDeltakere() {
  return useMutation({
    mutationFn: async ({
      orgnr,
      gjennomforingId,
      periodeStart,
      periodeSlutt,
    }: FetchDeltakereParams) => {
      const result = await ArrangorflateService.getOpprettKravDeltakere({
        path: { orgnr, gjennomforingId },
        query: { periodeStart, periodeSlutt },
        client: queryClient,
      });

      if (result.error) {
        throw result.error;
      }

      return result.data;
    },
  });
}
