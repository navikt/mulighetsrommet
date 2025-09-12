import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";

export function useAvtaleEndringshistorikk(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.avtaleHistorikk(id),
    queryFn() {
      return AvtaleService.getAvtaleEndringshistorikk({
        path: { id },
      });
    },
  });
}
