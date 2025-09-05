import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@mr/api-client-v2";

export function useArrangorHovedenhet(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.arrangorHovedenhetById(id),
    queryFn: () => {
      return ArrangorService.getArrangorHovedenhetById({ path: { id: id } });
    },
  });
}
