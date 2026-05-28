import { QueryKeys } from "@/api/query-keys";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { VeilederService } from "@arbeidsmarkedstiltak/api-client";

export function useVeilederdata() {
  return useApiSuspenseQuery({
    queryKey: [QueryKeys.Veilederdata],
    queryFn: () => VeilederService.getVeileder(),
  });
}
