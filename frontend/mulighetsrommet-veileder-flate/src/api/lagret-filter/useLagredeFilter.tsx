import { LagretDokumenttype, LagretFilterService } from "@mr/api-client-v2";
import { QueryKeys } from "../query-keys";
import { useApiQuery } from "@/hooks/useApiQuery";

export function useLagredeFilter(dokumenttype: LagretDokumenttype) {
  return useApiQuery({
    queryKey: QueryKeys.lagredeFilter(dokumenttype),
    queryFn: () => LagretFilterService.getMineFilterForDokumenttype({ path: { dokumenttype } }),
  });
}
