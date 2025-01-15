import { useQuery } from "@tanstack/react-query";
import { LagretDokumenttype, LagretFilterService } from "@mr/api-client";
import { QueryKeys } from "../QueryKeys";

export function useLagredeFilter(dokumenttype: LagretDokumenttype) {
  return useQuery({
    queryKey: QueryKeys.lagredeFilter(dokumenttype),
    queryFn: () => LagretFilterService.getMineFilterForDokumenttype({ dokumenttype }),
  });
}
