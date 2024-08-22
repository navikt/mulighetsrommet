import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "./useModiaContext";
import { DelMedBrukerService } from "@mr/api-client";

export function useDeltMedBrukerHistorikk() {
  const { fnr: norskIdent } = useModiaContext();

  return useQuery({
    queryKey: QueryKeys.deltMedBrukerHistorikk(norskIdent),
    queryFn: () => {
      return DelMedBrukerService.getHistorikkForDeltMedBruker({
        requestBody: {
          norskIdent,
        },
      });
    },
  });
}
