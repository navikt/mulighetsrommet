import { QueryKeys } from "@/api/QueryKeys";
import { PAGE_SIZE } from "@/constants";
import { TiltakstyperService } from "@mr/api-client-v2";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { TiltakstypeFilter } from "../atoms";

export function useTiltakstyper(filter: TiltakstypeFilter = {}, page: number = 1) {
  const queryFilter = {
    query: {
      sort: filter.sort?.sortString,
      page,
      size: PAGE_SIZE,
    },
  };

  return useApiSuspenseQuery({
    queryKey: QueryKeys.tiltakstyper(queryFilter),
    queryFn: () => TiltakstyperService.getTiltakstyper(queryFilter),
  });
}
