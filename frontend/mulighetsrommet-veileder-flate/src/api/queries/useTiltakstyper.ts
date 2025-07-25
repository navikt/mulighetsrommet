import { QueryKeys } from "../query-keys";
import { VeilederTiltakService } from "@api-client";
import { useApiSuspenseQuery } from "@mr/frontend-common";

export function useTiltakstyper() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.arbeidsmarkedstiltak.tiltakstyper,
    queryFn: () => VeilederTiltakService.getVeilederflateTiltakstyper(),
  });
}
