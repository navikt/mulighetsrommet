import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "./useModiaContext";
import { DelMedBrukerService } from "mulighetsrommet-api-client";

export function useHentAlleTiltakDeltMedBruker() {
  const { fnr: norskIdent } = useModiaContext();

  const { data: alleTiltakDeltMedBruker } = useQuery({
    queryKey: [QueryKeys.AlleDeltMedBrukerStatus, norskIdent],
    queryFn: async () => {
      const result = await DelMedBrukerService.getAlleTiltakDeltMedBruker({
        requestBody: { norskIdent },
      });
      return result || null; // Returner null hvis API returnerer 204 No Content = undefined;
    },
    throwOnError: false, // Er ingen krise hvis dette kallet feiler
  });

  return { alleTiltakDeltMedBruker };
}
