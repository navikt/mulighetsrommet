import { useQuery } from "@tanstack/react-query";
import { useFnr } from "../../../hooks/useFnr";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useHentHistorikk(prefetch: boolean = true) {
  const fnr = useFnr();

  const requestBody = { norskIdent: fnr };

  return useQuery({
    queryKey: [QueryKeys.Historikk, fnr],
    queryFn: () => mulighetsrommetClient.historikk.hentHistorikkForBruker({ requestBody }),
    enabled: prefetch,
  });
}
