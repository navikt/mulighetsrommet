import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@mr/api-client-v2";
import { useModiaContext } from "./useModiaContext";
import { useSuspenseQueryWrapper } from "@/hooks/useQueryWrapper";

export function useDeltMedBrukerHistorikk() {
  const { fnr: norskIdent } = useModiaContext();

  return useSuspenseQueryWrapper({
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
