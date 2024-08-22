import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { HistorikkService } from "@mr/api-client";
import { useGetTiltaksgjennomforingIdFraUrl } from "../../hooks/useGetTiltaksgjennomforingIdFraUrl";

export function useHentDeltakelseForGjennomforing() {
  const { fnr: norskIdent } = useModiaContext();
  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFraUrl();
  return useSuspenseQuery({
    queryKey: QueryKeys.DeltakelseForGjennomforing(norskIdent, tiltaksgjennomforingId),
    queryFn: () =>
      HistorikkService.hentDeltakelseForGjennomforing({
        requestBody: { norskIdent, tiltaksgjennomforingId },
      }),
  });
}
