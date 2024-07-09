import { useQuery } from "@tanstack/react-query";
import { LagretDokumenttype } from "mulighetsrommet-api-client";
import { LagretFilterService } from "mulighetsrommet-api-client";
import { QueryKeys } from "../../QueryKeys";

export function useGetLagredeFilterForDokumenttype(dokumenttype: LagretDokumenttype) {
  return useQuery({
    queryKey: QueryKeys.lagredeFilter(dokumenttype),
    queryFn: () => LagretFilterService.getMineFilterForDokumenttype({ dokumenttype }),
  });
}
