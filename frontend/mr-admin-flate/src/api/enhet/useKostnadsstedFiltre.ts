import { QueryKeys } from "@/api/QueryKeys";
import { NavEnheterService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useKostnadsstedFiltre() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.kostnadssted(),

    queryFn: () => {
      return NavEnheterService.getKostnadsstedFilter();
    },
  });
}
