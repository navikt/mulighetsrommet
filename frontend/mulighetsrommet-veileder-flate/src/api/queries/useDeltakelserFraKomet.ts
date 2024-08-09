import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { HistorikkService } from "mulighetsrommet-api-client";

export function useDeltakelserFraKomet() {
  const { fnr } = useModiaContext();

  const requestBody = { norskIdent: fnr };

  return useSuspenseQuery({
    queryKey: QueryKeys.BrukerDeltakelser(fnr),
    queryFn: () => HistorikkService.hentDeltakelserFraKomet({ requestBody }),
  });
}
