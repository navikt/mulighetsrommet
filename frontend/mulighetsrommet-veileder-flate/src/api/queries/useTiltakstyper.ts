import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { VeilederTiltakService } from "@mr/api-client";

export function useTiltakstyper() {
  return useSuspenseQuery({
    queryKey: QueryKeys.sanity.tiltakstyper,
    queryFn: () => VeilederTiltakService.getVeilederflateTiltakstyper(),
  });
}
