import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@mr/api-client-v2";
import { useQuery } from "@tanstack/react-query";

export function useHentDeltMedBrukerStatus(norskIdent: string, gjennomforingId: string) {
  return useQuery({
    queryKey: [...QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: async () => {
      const { data } = await DelMedBrukerService.getDelMedBruker({
        body: { norskIdent, tiltakId: gjennomforingId },
      });
      if (data && "id" in data) {
        return data;
      } else {
        return undefined;
      }
    },
    throwOnError: false,
  });
}
