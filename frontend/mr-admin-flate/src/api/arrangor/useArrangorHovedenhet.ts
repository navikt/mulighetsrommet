import { useApiQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@mr/api-client-v2";

export function useArrangorHovedenhet(id?: string) {
  return useApiQuery({
    queryKey: QueryKeys.arrangorHovedenhetById(id!),
    queryFn: () => {
      return ArrangorService.getArrangorHovedenhetById({ path: { id: id! } });
    },
    enabled: !!id,
  });
}
