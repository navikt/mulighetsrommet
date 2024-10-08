import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { NavEnheterService } from "@mr/api-client";

export function useRegioner() {
  return useSuspenseQuery({
    queryKey: QueryKeys.navRegioner,
    queryFn: () => {
      return NavEnheterService.getRegioner();
    },
  });
}
