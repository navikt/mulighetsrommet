import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { VirksomhetService } from "@mr/api-client";

export function useBrregVirksomhetUnderenheter(orgnr: string) {
  return useQuery({
    queryKey: QueryKeys.brregVirksomhetUnderenheter(orgnr),
    queryFn: () => {
      return VirksomhetService.getBrregVirksomhetUnderenheter({ orgnr });
    },
    enabled: !!orgnr,
  });
}
