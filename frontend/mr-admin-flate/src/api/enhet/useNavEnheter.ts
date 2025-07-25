import { QueryKeys } from "@/api/QueryKeys";
import { NavEnheterService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useNavEnheter() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.enheter({}),

    queryFn: () => {
      return NavEnheterService.getEnheter();
    },
  });
}
