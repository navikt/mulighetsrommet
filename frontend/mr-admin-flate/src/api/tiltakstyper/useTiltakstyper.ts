import { useApiQuery } from "@mr/frontend-common";
import { PAGE_SIZE } from "@/constants";
import { TiltakstypeFilter } from "../atoms";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstyperService } from "@mr/api-client-v2";

export function useTiltakstyper(filter: TiltakstypeFilter = {}, page: number = 1) {
  const queryFilter = {
    query: {
      sort: filter.sort?.sortString,
      page,
      size: PAGE_SIZE,
    },
  };

  return useApiQuery({
    queryKey: QueryKeys.tiltakstyper(queryFilter),
    queryFn: () => TiltakstyperService.getTiltakstyper(queryFilter),
  });
}
