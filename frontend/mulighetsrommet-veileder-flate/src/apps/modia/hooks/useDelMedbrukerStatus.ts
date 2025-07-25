import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@api-client";
import { useQuery } from "@tanstack/react-query";

export function useDelMedBrukerStatus(norskIdent: string, gjennomforingId: string) {
  return useQuery({
    queryKey: [...QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: async () => {
      const { data } = await DelMedBrukerService.getDelMedBruker({
        body: { norskIdent, tiltakId: gjennomforingId },
      });
      if (data && "id" in data) {
        return data;
      } else {
        return null;
      }
    },
    throwOnError: false,
  });
}
