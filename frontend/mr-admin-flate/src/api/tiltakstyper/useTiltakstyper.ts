import { useQuery } from "@tanstack/react-query";
import { PAGE_SIZE } from "@/constants";
import { TiltakstypeFilter } from "../atoms";
import { QueryKeys } from "@/api/QueryKeys";
import { TiltakstyperService } from "@mr/api-client";

export function useTiltakstyper(filter: TiltakstypeFilter = {}, page: number = 1) {
  const queryFilter = {
    sort: filter.sort?.sortString,
    page,
    size: PAGE_SIZE,
  };

  return useQuery({
    queryKey: QueryKeys.tiltakstyper(queryFilter),
    queryFn: () => TiltakstyperService.getTiltakstyper(queryFilter),
  });
}
