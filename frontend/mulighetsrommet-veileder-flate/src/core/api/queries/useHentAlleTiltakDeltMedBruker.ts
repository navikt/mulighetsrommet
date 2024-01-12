import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";
import { useAppContext } from "../../../hooks/useAppContext";

export function useHentAlleTiltakDeltMedBruker() {
  const { fnr: norskIdent } = useAppContext();

  const { data: alleTiltakDeltMedBruker } = useQuery({
    queryKey: [QueryKeys.AlleDeltMedBrukerStatus, norskIdent],
    queryFn: async () => {
      const result = await mulighetsrommetClient.delMedBruker.getAlleTiltakDeltMedBruker({
        requestBody: { norskIdent },
      });
      return result || null; // Returner null hvis API returnerer 204 No Content = undefined;
    },
    throwOnError: false, // Er ingen krise hvis dette kallet feiler
  });

  return { alleTiltakDeltMedBruker };
}
