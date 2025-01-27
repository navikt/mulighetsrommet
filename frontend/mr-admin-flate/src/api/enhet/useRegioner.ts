import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { NavEnheterService } from "@mr/api-client-v2";

export function useRegioner() {
  return useApiQuery({
    queryKey: QueryKeys.navRegioner(),
    queryFn: () => {
      return NavEnheterService.getRegioner();
    },
  });
}
