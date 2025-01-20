import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "@mr/api-client";
import { useQuery } from "@tanstack/react-query";
import { useTiltakIdFraUrl } from "../../hooks/useTiltakIdFraUrl";
import { QueryKeys } from "../query-keys";

export function useHentDeltakelseForGjennomforing() {
  const { fnr: norskIdent } = useModiaContext();
  const gjennomforingId = useTiltakIdFraUrl();
  return useQuery({
    queryKey: QueryKeys.DeltakelseForGjennomforing(norskIdent, gjennomforingId),
    queryFn: async () => {
      const result = await HistorikkService.hentDeltakelseForGjennomforing({
        requestBody: { norskIdent, gjennomforingId },
      });
      return result || null; // Returner null hvis API returnerer 204 No Content = undefined;;
    },
    throwOnError: false,
    enabled: !!gjennomforingId,
  });
}
