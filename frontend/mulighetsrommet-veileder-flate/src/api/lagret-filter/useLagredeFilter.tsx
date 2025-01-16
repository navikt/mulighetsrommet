import { LagretDokumenttype, LagretFilterService } from "@mr/api-client-v2";
import { QueryKeys } from "../query-keys";
import { useQueryWrapper } from "@/hooks/useQueryWrapper";

export function useLagredeFilter(dokumenttype: LagretDokumenttype) {
  return useQueryWrapper({
    queryKey: QueryKeys.lagredeFilter(dokumenttype),
    queryFn: () => LagretFilterService.getMineFilterForDokumenttype({ path: { dokumenttype } }),
  });
}
