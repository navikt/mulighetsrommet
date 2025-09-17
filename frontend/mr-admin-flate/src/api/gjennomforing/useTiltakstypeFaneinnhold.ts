import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstypeService } from "@tiltaksadministrasjon/api-client";

export function useTiltakstypeFaneinnhold(tiltakstypeId: string) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstypeFaneinnhold(tiltakstypeId),
    queryFn: () => TiltakstypeService.getTiltakstypeFaneinnhold({ path: { id: tiltakstypeId } }),
  });
}
