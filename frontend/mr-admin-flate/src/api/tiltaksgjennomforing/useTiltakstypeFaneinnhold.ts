import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstyperService } from "@mr/api-client";

export function useTiltakstypeFaneinnhold(tiltakstypeId: string) {
  return useSuspenseQuery({
    queryKey: QueryKeys.tiltakstypeFaneinnhold(tiltakstypeId),
    queryFn: () =>
      TiltakstyperService.getTiltakstypeFaneinnhold({
        id: tiltakstypeId,
      }),
  });
}
