import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@mr/api-client-v2";

export function useArrangorKontaktpersoner(arrangorId: string) {
  return useApiQuery({
    queryKey: QueryKeys.arrangorKontaktpersoner(arrangorId),

    queryFn: () => ArrangorService.getArrangorKontaktpersoner({ path: { id: arrangorId } }),

    enabled: !!arrangorId,
  });
}
