import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { KodeverkService } from "@tiltaksadministrasjon/api-client";

export function useKontorstruktur() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.navRegioner(),
    queryFn: () => {
      return KodeverkService.getKontorstruktur();
    },
  });
}
