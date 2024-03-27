import { useSuspenseQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../client";
import { QueryKeys } from "../query-keys";

export function useTiltakstyper() {
  return useSuspenseQuery({
    queryKey: QueryKeys.sanity.tiltakstyper,
    queryFn: () => mulighetsrommetClient.veilederTiltak.getVeilederflateTiltakstyper(),
  });
}
