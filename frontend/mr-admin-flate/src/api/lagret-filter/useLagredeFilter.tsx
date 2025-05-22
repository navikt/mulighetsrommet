import { useApiQuery } from "@mr/frontend-common";
import { LagretFilterService, LagretFilterType } from "@mr/api-client-v2";
import { QueryKeys } from "../QueryKeys";

export function useLagredeFilter(dokumenttype: LagretFilterType) {
  return useApiQuery({
    queryKey: QueryKeys.lagredeFilter(dokumenttype),
    queryFn: () => LagretFilterService.getMineFilterForDokumenttype({ path: { dokumenttype } }),
  });
}
