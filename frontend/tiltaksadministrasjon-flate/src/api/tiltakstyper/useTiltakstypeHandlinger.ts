import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstypeService } from "@tiltaksadministrasjon/api-client";

export function useTiltakstypeHandlinger() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  const { data } = useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstypeHandlinger(tiltakstypeId),
    queryFn: () => TiltakstypeService.getTiltakstypeHandlinger({ path: { id: tiltakstypeId } }),
  });

  return data;
}
