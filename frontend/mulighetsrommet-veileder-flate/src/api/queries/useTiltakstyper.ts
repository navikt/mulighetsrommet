import { QueryKeys } from "../query-keys";
import { VeilederTiltakService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useTiltakstyper() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltakstyper,
    queryFn: () => VeilederTiltakService.getVeilederflateTiltakstyper(),
  });
}
