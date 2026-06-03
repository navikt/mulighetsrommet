import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@tiltaksadministrasjon/api-client";

export function useSyncArrangorFromBrreg(orgnr: string) {
  return useApiQuery({
    queryKey: QueryKeys.arrangorByOrgnr(orgnr),
    queryFn: () => {
      return ArrangorService.syncArrangorFromBrreg({ path: { orgnr } });
    },
    enabled: !!orgnr,
  });
}
