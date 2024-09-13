import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@mr/api-client";
import { useSuspenseQuery } from "@tanstack/react-query";
import { useModiaContext } from "./useModiaContext";

export function useDeltMedBrukerHistorikk() {
  const { fnr: norskIdent } = useModiaContext();

  return useSuspenseQuery({
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
