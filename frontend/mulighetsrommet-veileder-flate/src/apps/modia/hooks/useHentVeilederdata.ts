import { QueryKeys } from "@/api/query-keys";
import { useApiSuspenseQuery } from "@/hooks/useApiQuery";
import { VeilederService } from "@mr/api-client-v2";

export function useHentVeilederdata() {
  return useApiSuspenseQuery({
    queryKey: [QueryKeys.Veilederdata],
    queryFn: () => VeilederService.getVeileder(),
  });
}
