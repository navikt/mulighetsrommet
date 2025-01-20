import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "@mr/api-client-v2";
import { useGetTiltakIdFraUrl } from "../../hooks/useGetTiltaksgjennomforingIdFraUrl";
import { QueryKeys } from "../query-keys";
import { useQuery } from "@tanstack/react-query";

export function useHentDeltakelseForGjennomforing() {
  const { fnr: norskIdent } = useModiaContext();

  const gjennomforingId = useGetTiltakIdFraUrl();
  return useQuery({
    queryKey: QueryKeys.DeltakelseForGjennomforing(norskIdent, gjennomforingId),
    queryFn: async () => {
      const result = await HistorikkService.hentDeltakelseForGjennomforing<false>({
        body: { norskIdent, gjennomforingId },
      });

      if (Object.prototype.hasOwnProperty.call(result, "id")) {
        return result.data;
      }
      return null; // Returner null hvis API returnerer 204 No Content = {};
    },
    throwOnError: false,
    enabled: !!gjennomforingId,
  });
}
