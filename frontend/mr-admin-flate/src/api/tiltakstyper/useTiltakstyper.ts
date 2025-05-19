import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstyperService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { TiltakstypeFilter } from "../atoms";

export function useTiltakstyper(filter: TiltakstypeFilter = {}) {
  const queryFilter = {
    query: {
      sort: filter.sort?.sortString,
    },
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstyper(queryFilter),
    queryFn: () => TiltakstyperService.getTiltakstyper(queryFilter),
  });
}
