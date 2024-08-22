import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { HistorikkService } from "@mr/api-client";

export function useDeltakelserFraKomet() {
  const { fnr: norskIdent } = useModiaContext();

  return useSuspenseQuery({
    queryKey: QueryKeys.BrukerDeltakelser(norskIdent),
    queryFn: () => HistorikkService.hentDeltakelserFraKomet({ requestBody: { norskIdent } }),
  });
}
