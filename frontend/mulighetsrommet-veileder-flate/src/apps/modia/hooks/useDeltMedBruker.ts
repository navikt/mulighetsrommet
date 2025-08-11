import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useDeltMedBruker(norskIdent: string, gjennomforingId: string) {
  return useApiSuspenseQuery({
    queryKey: [...QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: async () => {
      const result = await DelMedBrukerService.getDeltMedBruker<false>({
        body: { norskIdent, tiltakId: gjennomforingId },
      });
      if (result.response.status === 204) {
        return { data: null };
      } else {
        return { data: result.data };
      }
    },
    throwOnError: false,
  });
}
