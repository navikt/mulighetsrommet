import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "./useModiaContext";
import { DelMedBrukerService } from "@mr/api-client-v2";
import { useQuery } from "@tanstack/react-query";

export function useHentAlleTiltakDeltMedBruker() {
  const { fnr: norskIdent } = useModiaContext();

  const { data } = useQuery({
    queryKey: [QueryKeys.AlleDeltMedBrukerStatus, norskIdent],
    queryFn: async () => {
      const result = await DelMedBrukerService.getAlleTiltakDeltMedBruker<false>({
        body: { norskIdent },
      });
      return result || null; // Returner null hvis API returnerer 204 No Content = undefined;
    },
    throwOnError: false, // Er ingen krise hvis dette kallet feiler
  });

  return { alleTiltakDeltMedBruker: data?.data };
}
