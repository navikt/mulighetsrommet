import { QueryKeys } from "../query-keys";
import { NavEnheterService } from "@mr/api-client-v2";
import { useSuspenseQueryWrapper } from "@/hooks/useQueryWrapper";

export function useRegioner() {
  return useSuspenseQueryWrapper({
    queryKey: QueryKeys.navRegioner,
    queryFn: () => {
      return NavEnheterService.getRegioner();
    },
  });
}
