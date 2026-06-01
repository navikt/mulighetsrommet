import { QueryKeys } from "@/api/QueryKeys";
import { KodeverkService } from "@tiltaksadministrasjon/api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useKostnadssteder() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.kostnadssted(),

    queryFn: () => {
      return KodeverkService.getKostnadssteder();
    },
  });
}
