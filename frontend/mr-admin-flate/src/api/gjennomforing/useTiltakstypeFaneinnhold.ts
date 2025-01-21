import { useApiSuspenseQuery } from "@/hooks/useApiQuery";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstyperService } from "@mr/api-client-v2";

export function useTiltakstypeFaneinnhold(tiltakstypeId: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstypeFaneinnhold(tiltakstypeId),
    queryFn: () =>
      TiltakstyperService.getTiltakstypeFaneinnhold({
        path: { id: tiltakstypeId },
      }),
  });
}
