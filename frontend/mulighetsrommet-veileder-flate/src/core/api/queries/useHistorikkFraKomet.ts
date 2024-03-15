import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useSuspenseQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useHistorikkFraKomet() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };
  return useSuspenseQuery({
    queryKey: [QueryKeys.HistorikkFraKomet, fnr],
    queryFn: () => mulighetsrommetClient.historikk.hentHistorikkForBrukerFraKomet({ requestBody }),
  });
}
