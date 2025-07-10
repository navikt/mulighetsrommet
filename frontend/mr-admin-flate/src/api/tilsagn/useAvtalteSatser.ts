import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { AvtalerService } from "@mr/api-client-v2";

export function useAvtalteSatser(avtaleId: string) {
  return useApiQuery({
    queryFn: () => AvtalerService.getAvtalteSatser({ path: { id: avtaleId } }),
    queryKey: QueryKeys.avtalteSatser(avtaleId),
  });
}
