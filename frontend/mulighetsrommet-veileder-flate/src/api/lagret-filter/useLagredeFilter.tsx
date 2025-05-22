import { LagretFilterType, LagretFilterService } from "@mr/api-client-v2";
import { QueryKeys } from "../query-keys";
import { useApiQuery } from "@mr/frontend-common";

export function useLagredeFilter(dokumenttype: LagretFilterType) {
  return useApiQuery({
    queryKey: QueryKeys.lagredeFilter(dokumenttype),
    queryFn: () => LagretFilterService.getMineFilterForDokumenttype({ path: { dokumenttype } }),
  });
}
