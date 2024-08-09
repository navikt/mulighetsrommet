import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "mulighetsrommet-api-client";

export function useTiltakshistorikkForBruker() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };

  return useQuery({
    queryKey: QueryKeys.BrukerHistorikk(fnr),
    queryFn: () => HistorikkService.hentHistorikkForBrukerV2({ requestBody }),
  });
}
