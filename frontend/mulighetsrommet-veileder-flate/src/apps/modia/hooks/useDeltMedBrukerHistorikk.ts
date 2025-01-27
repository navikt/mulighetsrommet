import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@mr/api-client-v2";
import { useModiaContext } from "./useModiaContext";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useDeltMedBrukerHistorikk() {
  const { fnr: norskIdent } = useModiaContext();

  return useApiSuspenseQuery({
    queryKey: QueryKeys.deltMedBrukerHistorikk(norskIdent),
    queryFn: () => {
      return DelMedBrukerService.getHistorikkForDeltMedBruker({
        body: {
          norskIdent,
        },
      });
    },
  });
}
