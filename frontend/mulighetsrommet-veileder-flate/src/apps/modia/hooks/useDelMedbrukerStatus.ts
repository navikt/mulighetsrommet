import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useDelMedBrukerStatus(norskIdent: string, gjennomforingId: string) {
  return useApiSuspenseQuery({
    queryKey: [...QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: async () => {
      const result = await DelMedBrukerService.getDelMedBruker({
        body: { norskIdent, tiltakId: gjennomforingId },
      });
      return { data: result.data ?? null };
    },
    throwOnError: false,
  });
}
