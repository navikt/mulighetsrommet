import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AvtaleService } from "@tiltaksadministrasjon/api-client";

export function useAvtalteSatser(avtaleId: string) {
  return useApiQuery({
    queryFn: () => AvtaleService.getAvtalteSatser({ path: { id: avtaleId } }),
    queryKey: QueryKeys.avtalteSatser(avtaleId),
  });
}
