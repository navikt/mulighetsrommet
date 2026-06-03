import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@tiltaksadministrasjon/api-client";

export function useArrangor(id: string) {
  return useApiQuery({
    queryKey: QueryKeys.arrangorById(id),
    queryFn: () => {
      return ArrangorService.getArrangor({ path: { id } });
    },
  });
}
