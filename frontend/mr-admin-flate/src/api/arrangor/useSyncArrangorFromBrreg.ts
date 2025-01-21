import { useApiQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@mr/api-client-v2";

export function useSyncArrangorFromBrreg(orgnr: string) {
  return useApiQuery({
    queryKey: QueryKeys.arrangorByOrgnr(orgnr),
    queryFn: () => {
      return ArrangorService.syncArrangorFromBrreg({ path: { orgnr } });
    },
    enabled: !!orgnr,
  });
}
