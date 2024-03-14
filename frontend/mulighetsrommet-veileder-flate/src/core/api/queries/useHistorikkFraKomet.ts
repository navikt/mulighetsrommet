import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { mulighetsrommetClient } from "../clients";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";

export function useHistorikkFraKomet() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };
  return useQuery({
    queryKey: [QueryKeys.HistorikkFraKomet, fnr],
    queryFn: () => mulighetsrommetClient.historikk.hentHistorikkForBrukerFraKomet({ requestBody }),
    throwOnError: true, // TODO Fjern denne når vi har endepunkt fra Komet
  });
}
