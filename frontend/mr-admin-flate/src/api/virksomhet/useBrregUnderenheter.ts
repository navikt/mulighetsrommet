import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { BrregService } from "@mr/api-client-v2";

export function useBrregUnderenheter(orgnr: string) {
  return useApiQuery({
    queryKey: QueryKeys.brregVirksomhetUnderenheter(orgnr),
    queryFn: () => {
      return BrregService.getBrregUnderenheter({ path: { orgnr } });
    },
    enabled: !!orgnr,
  });
}
