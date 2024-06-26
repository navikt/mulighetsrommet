import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useSuspenseQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../client";
import { QueryKeys } from "../query-keys";

export function useDeltakelserFraKomet() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };
  return useSuspenseQuery({
    queryKey: [QueryKeys.HistorikkV2, fnr],
    queryFn: () => mulighetsrommetClient.historikk.hentDeltakelserFraKomet({ requestBody }),
  });
}
