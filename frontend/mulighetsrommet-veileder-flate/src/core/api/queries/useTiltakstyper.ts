import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useTiltakstyper() {
  return useQuery({
    queryKey: QueryKeys.sanity.tiltakstyper,
    queryFn: () => mulighetsrommetClient.veileder.getVeilederflateTiltakstyper(),
  });
}
