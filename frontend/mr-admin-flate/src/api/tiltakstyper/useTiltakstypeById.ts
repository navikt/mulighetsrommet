import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstypeService } from "@tiltaksadministrasjon/api-client";

export function useTiltakstypeById() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstype(tiltakstypeId),
    queryFn: () => TiltakstypeService.getTiltakstype({ path: { id: tiltakstypeId } }),
    staleTime: 1000,
  });
}
