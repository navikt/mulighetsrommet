import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "@mr/api-client";
import { useSuspenseQuery } from "@tanstack/react-query";

export function useTiltakshistorikkForBruker() {
  const { fnr: norskIdent } = useModiaContext();

  return useSuspenseQuery({
    queryKey: QueryKeys.BrukerHistorikk(norskIdent),
    queryFn: () => HistorikkService.hentHistorikkForBruker({ requestBody: { norskIdent } }),
  });
}
