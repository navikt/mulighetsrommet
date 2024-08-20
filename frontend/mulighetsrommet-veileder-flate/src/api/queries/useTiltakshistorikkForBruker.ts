import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "@mr/api-client";

export function useTiltakshistorikkForBruker() {
  const { fnr: norskIdent } = useModiaContext();

  return useQuery({
    queryKey: QueryKeys.BrukerHistorikk(norskIdent),
    queryFn: () => HistorikkService.hentHistorikkForBruker({ requestBody: { norskIdent } }),
  });
}
