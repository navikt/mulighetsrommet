import { useApiQuery } from "@/hooks/useApiQuery";
import { useGetTiltakstypeIdFromUrlOrThrow } from "../../hooks/useGetTiltakstypeIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstyperService } from "@mr/api-client-v2";

export function useTiltakstypeById() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  return useApiQuery({
    queryKey: QueryKeys.tiltakstype(tiltakstypeId),
    queryFn: () =>
      TiltakstyperService.getTiltakstypeById({
        path: { id: tiltakstypeId },
      }),
    staleTime: 1000,
  });
}
