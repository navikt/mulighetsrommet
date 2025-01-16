import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@mr/api-client-v2";
import { useQuery } from "@tanstack/react-query";

export function useHentDeltMedBrukerStatus(norskIdent: string, gjennomforingId: string) {
  const { data: delMedBrukerInfo } = useQuery({
    queryKey: [...QueryKeys.DeltMedBrukerStatus, norskIdent, gjennomforingId],
    queryFn: async () => {
      const result = await DelMedBrukerService.getDelMedBruker({
        body: { norskIdent, tiltakId: gjennomforingId },
      });

      if (Object.prototype.hasOwnProperty.call(result, "id")) {
        return result.data;
      }
      return null; // Returner null hvis API returnerer 204 No Content = {};
    },
  });

  return { delMedBrukerInfo };
}
