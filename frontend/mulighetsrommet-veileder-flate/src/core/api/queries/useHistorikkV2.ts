import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useSuspenseQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useHistorikkV2() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };
  return useSuspenseQuery({
    queryKey: [QueryKeys.HistorikkV2, fnr],
    queryFn: () => mulighetsrommetClient.historikk.hentHistorikkForBrukerV2({ requestBody }),
  });
}
