import { QueryKeys } from "@/api/query-keys";
import { DelMedBrukerService } from "@arbeidsmarkedstiltak/api-client";
import { useModiaContext } from "./useModiaContext";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useDeltMedBrukerHistorikk() {
  const { fnr: norskIdent } = useModiaContext();

  return useApiSuspenseQuery({
    queryKey: QueryKeys.deltMedBrukerHistorikk(norskIdent),
    queryFn: () => {
      return DelMedBrukerService.getDeltMedBrukerHistorikk({
        body: {
          norskIdent,
        },
      });
    },
  });
}
