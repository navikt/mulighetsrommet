import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@arbeidsmarkedstiltak/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useLastDelMedBruker(norskIdent: string, gjennomforingId: string) {
  return useApiSuspenseQuery({
    queryKey: [...QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: async () => {
      const result = await DelMedBrukerService.getLastDelMedBruker({
        body: { norskIdent, tiltakId: gjennomforingId },
        throwOnError: true,
      });
      if (result.response.status === 204) {
        return { data: null };
      } else {
        return { data: result.data };
      }
    },
  });
}
