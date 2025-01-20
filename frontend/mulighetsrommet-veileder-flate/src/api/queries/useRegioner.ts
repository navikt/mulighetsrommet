import { QueryKeys } from "../query-keys";
import { NavEnheterService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@/hooks/useApiQuery";

export function useRegioner() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.navRegioner,
    queryFn: () => {
      return NavEnheterService.getRegioner();
    },
  });
}
