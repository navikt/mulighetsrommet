import { useApiQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { BrregService } from "@tiltaksadministrasjon/api-client";

export function useBrregUnderenheter(orgnr: string) {
  return useApiQuery({
    queryKey: QueryKeys.brregVirksomhetUnderenheter(orgnr),
    queryFn: () => {
      return BrregService.getBrregUnderenheter({ path: { orgnr } });
    },
    enabled: !!orgnr,
  });
}
