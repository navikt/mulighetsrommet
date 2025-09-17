import { QueryKeys } from "@/api/QueryKeys";
import { NavEnheterService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useNavEnheter() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.navEnheter(),

    queryFn: () => {
      return NavEnheterService.getEnheter();
    },
  });
}
