import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../../QueryKeys";
import { LagretDokumenttype, LagretFilterService } from "@mr/api-client-v2";

export function useGetLagredeFilterForDokumenttype(dokumenttype: LagretDokumenttype) {
  const query = useQuery({
    queryKey: QueryKeys.lagredeFilter(dokumenttype),
    queryFn: () => LagretFilterService.getMineFilterForDokumenttype<true>({ path: { dokumenttype } }),
  });

  return {
    ...query,
    data: query.data?.data,
  };
}
