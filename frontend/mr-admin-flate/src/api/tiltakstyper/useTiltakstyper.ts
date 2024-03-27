import { useQuery } from "@tanstack/react-query";
import { PAGE_SIZE } from "../../constants";
import { TiltakstypeFilter } from "../atoms";
import { mulighetsrommetClient } from "@/api/client";
import { QueryKeys } from "@/api/QueryKeys";

export function useTiltakstyper(filter: TiltakstypeFilter = {}, page: number = 1) {
  const queryFilter = {
    sort: filter.sort,
    page,
    size: PAGE_SIZE,
  };

  return useQuery({
    queryKey: QueryKeys.tiltakstyper(queryFilter),
    queryFn: () => mulighetsrommetClient.tiltakstyper.getTiltakstyper(queryFilter),
  });
}
