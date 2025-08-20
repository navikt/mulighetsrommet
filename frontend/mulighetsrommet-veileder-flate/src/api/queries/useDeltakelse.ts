import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "@api-client";
import { QueryKeys } from "../query-keys";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
import { useQuery } from "@tanstack/react-query";

export function useDeltakelse() {
  const { fnr: norskIdent } = useModiaContext();

  const tiltakId = useTiltakIdFraUrl();
  return useQuery({
    queryKey: QueryKeys.Deltakelse(norskIdent, tiltakId),
    queryFn: async () => {
      const { data } = await HistorikkService.hentDeltakelse({
        body: { norskIdent, tiltakId },
      });
      if ("id" in data) {
        return data;
      } else {
        return null;
      }
    },
    throwOnError: false,
    enabled: !!tiltakId,
  });
}
