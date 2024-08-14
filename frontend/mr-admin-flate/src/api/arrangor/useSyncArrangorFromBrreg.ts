import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { ArrangorService } from "@mr/api-client";

export function useSyncArrangorFromBrreg(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.arrangorByOrgnr(orgnr),
    queryFn: () => {
      return ArrangorService.syncArrangorFromBrreg({ orgnr });
    },
    enabled: !!orgnr,
  });
}
