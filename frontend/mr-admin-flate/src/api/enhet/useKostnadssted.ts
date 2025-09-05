import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { NavEnheterService } from "@tiltaksadministrasjon/api-client";

export function useKostnadssted(regioner: string[]) {
  return useApiQuery({
    queryKey: QueryKeys.kostnadssted(regioner),

    queryFn: () => {
      return NavEnheterService.getKostnadssted({ query: { regioner } });
    },
  });
}
