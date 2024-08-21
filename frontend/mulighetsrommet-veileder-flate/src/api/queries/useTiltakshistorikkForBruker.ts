import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "@mr/api-client";
import { useSuspenseQuery } from "@tanstack/react-query";

export function useTiltakshistorikkForBruker() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };

  return useSuspenseQuery({
    queryKey: QueryKeys.BrukerHistorikk(fnr),
    queryFn: () => HistorikkService.hentHistorikkForBruker({ requestBody }),
  });
}
