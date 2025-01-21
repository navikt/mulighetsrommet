import { QueryKeys } from "@/api/query-keys";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { VeilederService } from "@mr/api-client-v2";

export function useVeilederdata() {
  return useApiSuspenseQuery({
    queryKey: [QueryKeys.Veilederdata],
    queryFn: () => VeilederService.getVeileder(),
  });
}
