import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AvtalerService } from "@mr/api-client-v2";

export function useAvtaleEndringshistorikk(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.avtaleHistorikk(id),
    queryFn() {
      return AvtalerService.getAvtaleEndringshistorikk({
        path: { id },
      });
    },
  });
}
