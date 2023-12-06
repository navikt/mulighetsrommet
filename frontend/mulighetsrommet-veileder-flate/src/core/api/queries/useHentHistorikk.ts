import { useQuery } from "@tanstack/react-query";
import { useAppContext } from "../../../hooks/useAppContext";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useHentHistorikk(prefetch: boolean = true) {
  const { fnr } = useAppContext();

  const requestBody = { norskIdent: fnr };

  return useQuery({
    queryKey: [QueryKeys.Historikk, fnr],
    queryFn: () => mulighetsrommetClient.historikk.hentHistorikkForBruker({ requestBody }),
    enabled: prefetch,
  });
}
