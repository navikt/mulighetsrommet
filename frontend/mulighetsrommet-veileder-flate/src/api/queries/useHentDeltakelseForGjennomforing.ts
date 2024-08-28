import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "@mr/api-client";
import { useQuery } from "@tanstack/react-query";
import { useGetTiltaksgjennomforingIdFraUrl } from "../../hooks/useGetTiltaksgjennomforingIdFraUrl";
import { QueryKeys } from "../query-keys";

export function useHentDeltakelseForGjennomforing() {
  const { fnr: norskIdent } = useModiaContext();
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFraUrl();
  return useQuery({
    queryKey: QueryKeys.DeltakelseForGjennomforing(norskIdent, tiltaksgjennomforingId),
    queryFn: async () => {
      const result = await HistorikkService.hentDeltakelseForGjennomforing({
        requestBody: { norskIdent, tiltaksgjennomforingId },
      });
      return result || null; // Returner null hvis API returnerer 204 No Content = undefined;;
    },
    throwOnError: false,
    enabled: !!tiltaksgjennomforingId,
  });
}
