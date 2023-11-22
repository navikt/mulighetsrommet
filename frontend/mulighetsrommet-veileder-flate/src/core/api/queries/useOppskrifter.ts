import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../query-keys";
import { mulighetsrommetClient } from "../clients";

export function useOppskrifter(tiltakstypeId?: string) {
  return useQuery({
    queryKey: QueryKeys.oppskrifter(tiltakstypeId!!),
    queryFn: () => mulighetsrommetClient.oppskrifter.getOppskrifter({ tiltakstypeId }),
    enabled: !!tiltakstypeId,
  });
}
