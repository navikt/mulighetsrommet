import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useSuspenseQueryWrapper } from "@/hooks/useQueryWrapper";
import { HistorikkService } from "@mr/api-client-v2";

export function useTiltakshistorikkForBruker(type: "AKTIVE" | "HISTORISKE") {
  const { fnr: norskIdent } = useModiaContext();

  return useSuspenseQueryWrapper({
    queryKey: QueryKeys.BrukerHistorikk(norskIdent, type),
    queryFn: () => HistorikkService.getTiltakshistorikk({ body: { norskIdent, type } }),
  });
}
