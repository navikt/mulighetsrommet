import { QueryKeys } from "../query-keys";
import { NavEnheterService } from "@api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useRegioner() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.navRegioner,
    queryFn: () => {
      return NavEnheterService.getRegioner();
    },
  });
}
