import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@mr/api-client-v2";

export function useArrangor(id: string) {
  return useApiQuery({
    queryKey: QueryKeys.arrangorById(id),
    queryFn: () => {
      return ArrangorService.getArrangorById({ path: { id } });
    },
  });
}
