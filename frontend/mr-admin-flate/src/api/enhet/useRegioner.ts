import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { NavEnheterService } from "@mr/api-client";

export function useRegioner() {
  return useQuery({
    queryKey: QueryKeys.navRegioner(),
    queryFn: () => {
      return NavEnheterService.getRegioner();
    },
  });
}
