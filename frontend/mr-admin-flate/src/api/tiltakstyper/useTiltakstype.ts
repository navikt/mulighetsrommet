import { useApiSuspenseQuery } from "@mr/frontend-common";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstypeService } from "@tiltaksadministrasjon/api-client";

export function useTiltakstype(tiltakstypeId: string) {
  const { data: tiltakstype } = useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstype(tiltakstypeId),
    queryFn: () => TiltakstypeService.getTiltakstype({ path: { id: tiltakstypeId } }),
    staleTime: Infinity,
  });
  return tiltakstype;
}
