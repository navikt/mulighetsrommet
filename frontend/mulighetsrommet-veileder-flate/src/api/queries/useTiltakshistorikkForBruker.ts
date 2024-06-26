import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/query-keys";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";

export function useTiltakshistorikkForBruker() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };

  return useQuery({
    queryKey: [QueryKeys.Historikk, fnr],
    queryFn: () => mulighetsrommetClient.historikk.hentHistorikkForBruker({ requestBody }),
  });
}
