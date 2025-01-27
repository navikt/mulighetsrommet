import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { VirksomhetService } from "@mr/api-client-v2";

export function useBrregVirksomhetUnderenheter(orgnr: string) {
  return useApiQuery({
    queryKey: QueryKeys.brregVirksomhetUnderenheter(orgnr),
    queryFn: () => {
      return VirksomhetService.getBrregVirksomhetUnderenheter({ path: { orgnr } });
    },
    enabled: !!orgnr,
  });
}
