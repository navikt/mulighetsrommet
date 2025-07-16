import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { BrukerDeltakelseType, HistorikkService } from "@mr/api-client-v2";

export function useTiltakshistorikkForBruker(type: BrukerDeltakelseType) {
  const { fnr: norskIdent } = useModiaContext();

  return useApiSuspenseQuery({
    queryKey: QueryKeys.BrukerHistorikk(norskIdent, type),
    queryFn: () => HistorikkService.getTiltakshistorikk({ body: { norskIdent, type } }),
  });
}
