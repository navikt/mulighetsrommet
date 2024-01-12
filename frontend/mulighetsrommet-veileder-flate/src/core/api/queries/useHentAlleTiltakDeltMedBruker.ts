import { useQuery } from "@tanstack/react-query";
import { erPreview } from "../../../utils/Utils";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useHentAlleTiltakDeltMedBruker(norskIdent: string) {
  const { data: alleTiltakDeltMedBruker } = useQuery({
    queryKey: [QueryKeys.AlleDeltMedBrukerStatus, norskIdent],
    queryFn: async () => {
      const result = await mulighetsrommetClient.delMedBruker.getAlleTiltakDeltMedBruker({
        requestBody: { norskIdent },
      });
      return result || null; // Returner null hvis API returnerer 204 No Content = undefined;
    },
    enabled: !erPreview(),
    throwOnError: false, // Er ingen krise hvis dette kallet feiler
  });

  return { alleTiltakDeltMedBruker };
}
