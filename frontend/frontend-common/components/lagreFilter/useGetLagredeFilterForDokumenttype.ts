import { useQuery } from "@tanstack/react-query";
import { LagretDokumenttype } from "@mr/api-client";
import { LagretFilterService } from "@mr/api-client";
import { QueryKeys } from "../../QueryKeys";

export function useGetLagredeFilterForDokumenttype(dokumenttype: LagretDokumenttype) {
  return useQuery({
    queryKey: QueryKeys.lagredeFilter(dokumenttype),
    queryFn: () => LagretFilterService.getMineFilterForDokumenttype({ dokumenttype }),
  });
}
