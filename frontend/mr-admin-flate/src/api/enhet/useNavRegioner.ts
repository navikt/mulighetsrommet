import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { NavEnheterService } from "@tiltaksadministrasjon/api-client";

export function useNavRegioner() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.navRegioner(),
    queryFn: () => {
      return NavEnheterService.getRegioner();
    },
  });
}
