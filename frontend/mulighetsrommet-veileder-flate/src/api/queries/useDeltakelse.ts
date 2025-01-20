import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { HistorikkService } from "@mr/api-client-v2";
import { useGetTiltakIdFraUrl } from "../../hooks/useGetTiltaksgjennomforingIdFraUrl";
import { QueryKeys } from "../query-keys";
import { useApiQuery } from "@/hooks/useApiQuery";

export function useDeltakelse() {
  const { fnr: norskIdent } = useModiaContext();

  const tiltakId = useGetTiltakIdFraUrl();
  return useApiQuery({
    queryKey: QueryKeys.Deltakelse(norskIdent, tiltakId),
    queryFn: async () =>
      HistorikkService.hentDeltakelse({
        body: { norskIdent, tiltakId },
      }),
    throwOnError: false,
    enabled: !!tiltakId,
  });
}
