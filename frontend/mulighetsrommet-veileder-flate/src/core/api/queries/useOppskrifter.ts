import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../clients";
import { QueryKeys } from "../query-keys";

export function useOppskrifter(tiltakstypeId?: string) {
  return useQuery({
    queryKey: QueryKeys.oppskrifter(tiltakstypeId!!),
    queryFn: () =>
      mulighetsrommetClient.oppskrifter.getOppskrifter({
        tiltakstypeId: tiltakstypeId!!,
        perspective: "published",
      }),
    enabled: !!tiltakstypeId,
  });
}
