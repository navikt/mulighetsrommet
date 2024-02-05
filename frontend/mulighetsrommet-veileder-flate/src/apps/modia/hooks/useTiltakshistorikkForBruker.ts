import { useQuery } from "@tanstack/react-query";
import { useModiaContext } from "./useModiaContext";
import { mulighetsrommetClient } from "@/core/api/clients";
import { QueryKeys } from "@/core/api/query-keys";

export function useTiltakshistorikkForBruker() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };

  return useQuery({
    queryKey: [QueryKeys.Historikk, fnr],
    queryFn: () => mulighetsrommetClient.historikk.hentHistorikkForBruker({ requestBody }),
  });
}
