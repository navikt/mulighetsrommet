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
    queryFn: () =>
      HistorikkService.hentDeltakelseForGjennomforing({
        requestBody: { norskIdent, tiltaksgjennomforingId },
      }),
    enabled: !!tiltaksgjennomforingId,
  });
}
