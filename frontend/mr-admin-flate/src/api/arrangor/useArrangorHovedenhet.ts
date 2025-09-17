import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@tiltaksadministrasjon/api-client";

export function useArrangorHovedenhet(id: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.arrangorHovedenhetById(id),
    queryFn: () => {
      return ArrangorService.getArrangorHovedenhet({ path: { id: id } });
    },
  });
}
