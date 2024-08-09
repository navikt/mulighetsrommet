import { useSuspenseQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { VeilederTiltakService } from "mulighetsrommet-api-client";

export function useTiltakstyper() {
  return useSuspenseQuery({
    queryKey: QueryKeys.sanity.tiltakstyper,
    queryFn: () => VeilederTiltakService.getVeilederflateTiltakstyper(),
  });
}
