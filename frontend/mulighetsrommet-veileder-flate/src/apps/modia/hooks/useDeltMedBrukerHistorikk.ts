import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../../../api/client";
import { QueryKeys } from "../../../api/query-keys";
import { useModiaContext } from "./useModiaContext";

export function useDeltMedBrukerHistorikk() {
  const { fnr: norskIdent } = useModiaContext();

  return useQuery({
    queryKey: QueryKeys.deltMedBrukerHistorikk(norskIdent),
    queryFn: async () => {
      return await mulighetsrommetClient.delMedBruker.getHistorikkForDeltMedBruker({
        requestBody: {
          norskIdent,
        },
      });
    },
  });
}
